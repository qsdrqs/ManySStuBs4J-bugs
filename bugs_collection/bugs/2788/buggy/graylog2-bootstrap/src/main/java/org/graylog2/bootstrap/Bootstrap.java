/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bootstrap;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.log4j.InstrumentedAppender;
import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.Repository;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.guice.NamedConfigParametersModule;
import com.github.joschi.jadconfig.jodatime.JodaTimeConverterFactory;
import com.github.joschi.jadconfig.repositories.EnvironmentRepository;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;
import com.github.joschi.jadconfig.repositories.SystemPropertiesRepository;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.*;
import com.google.inject.name.Names;
import com.google.inject.spi.Message;
import io.airlift.command.Option;
import org.apache.log4j.Level;
import org.graylog2.UI;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginLoaderConfig;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeIdPersistenceException;
import org.graylog2.shared.bindings.GenericBindings;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.GuiceInstantiationService;
import org.graylog2.shared.bindings.InstantiationService;
import org.graylog2.shared.initializers.ServiceManagerListener;
import org.graylog2.shared.journal.KafkaJournalModule;
import org.graylog2.shared.journal.NoopJournalModule;
import org.graylog2.shared.plugins.LegacyPluginLoader;
import org.graylog2.shared.plugins.PluginLoader;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Strings.nullToEmpty;

public abstract class Bootstrap implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    protected static final String ENVIRONMENT_PREFIX = "GRAYLOG2_";
    protected static final String PROPERTIES_PREFIX = "graylog2.";
    protected static final Version version = Version.CURRENT_CLASSPATH;
    protected static final String FILE_SEPARATOR = System.getProperty("file.separator");
    protected static final String TMPDIR = System.getProperty("java.io.tmpdir", "/tmp");

    private final String commandName;
    private final BaseConfiguration configuration;

    public Bootstrap(String commandName, BaseConfiguration configuration) {
        this.commandName = commandName;
        this.configuration = configuration;
    }

    @Option(name = "--dump-config", description = "Show the effective Graylog2 configuration and exit")
    protected boolean dumpConfig = false;

    @Option(name = "--dump-default-config", description = "Show the default configuration and exit")
    protected boolean dumpDefaultConfig = false;

    @Option(name = {"-d", "--debug"}, description = "Run Graylog2 in debug mode")
    private boolean debug = false;

    @Option(name = {"-f", "--configfile"}, description = "Configuration file for Graylog2")
    private String configFile = "/etc/graylog2.conf";

    @Option(name = {"-p", "--pidfile"}, description = "File containing the PID of Graylog2")
    private String pidFile = TMPDIR + FILE_SEPARATOR + "graylog2.pid";

    @Option(name = {"-np", "--no-pid-file"}, description = "Do not write a PID file (overrides -p/--pidfile)")
    private boolean noPidFile = false;

    protected abstract void startNodeRegistration(Injector injector);
    protected abstract List<Module> getCommandBindings();
    protected abstract List<Object> getCommandConfigurationBeans();
    protected abstract Class<? extends Runnable> shutdownHook();

    protected abstract boolean validateConfiguration();

    public boolean isDumpConfig() {
        return dumpConfig;
    }

    public boolean isDumpDefaultConfig() {
        return dumpDefaultConfig;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getConfigFile() {
        return configFile;
    }

    public String getPidFile() {
        return pidFile;
    }

    public boolean isNoPidFile() {
        return noPidFile;
    }

    @Override
    public void run() {
        // Are we in debug mode?
        Level logLevel = Level.INFO;
        if (isDebug()) {
            LOG.info("Running in Debug mode");
            logLevel = Level.DEBUG;
        }
        org.apache.log4j.Logger.getRootLogger().setLevel(logLevel);
        org.apache.log4j.Logger.getLogger(Main.class.getPackage().getName()).setLevel(logLevel);

        String pluginPath = getPluginPath(getConfigFile());

        final JadConfig jadConfig = new JadConfig();
        jadConfig.addConverterFactory(new JodaTimeConverterFactory());

        final Set<PluginModule> pluginModules = loadPluginModules(pluginPath);

        for (PluginModule pluginModule : pluginModules)
            for (PluginConfigBean configBean : pluginModule.getConfigBeans())
                jadConfig.addConfigurationBean(configBean);

        if (isDumpDefaultConfig()) {
            for (Object bean : getCommandConfigurationBeans())
                jadConfig.addConfigurationBean(bean);
            System.out.println(dumpConfiguration(jadConfig.dump()));
            System.exit(0);
        }

        final NamedConfigParametersModule configModule = readConfiguration(jadConfig, getConfigFile());

        if (isDumpConfig()) {
            System.out.println(dumpConfiguration(jadConfig.dump()));
            System.exit(0);
        }

        if (!validateConfiguration()) {
            LOG.error("Validating configuration file failed - exiting.");
            System.exit(1);
        }

        final Injector injector = setupInjector(configModule, pluginModules);

        if (injector == null) {
            LOG.error("Injector could not be created, exiting! (Please include the previous error messages in bug reports.)");
            System.exit(1);
        }

        // This is holding all our metrics.
        final MetricRegistry metrics = injector.getInstance(MetricRegistry.class);

        // Report metrics via JMX.
        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();

        InstrumentedAppender logMetrics = new InstrumentedAppender(metrics);
        logMetrics.activateOptions();
        org.apache.log4j.Logger.getRootLogger().addAppender(logMetrics);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        LOG.info("Graylog2 " + commandName + " {} starting up. (JRE: {})", version, Tools.getSystemInformation());

        // Do not use a PID file if the user requested not to
        if (!isNoPidFile()) {
            savePidFile(getPidFile());
        }

        final ServerStatus serverStatus = injector.getInstance(ServerStatus.class);
        serverStatus.initialize();

        startNodeRegistration(injector);

        final ActivityWriter activityWriter;
        final ServiceManager serviceManager;
        try {
            activityWriter = injector.getInstance(ActivityWriter.class);
            serviceManager = injector.getInstance(ServiceManager.class);
        } catch (ProvisionException e) {
            LOG.error("Guice error", e);
            annotateProvisionException(e);
            System.exit(-1);
            return;
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            System.exit(-1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(injector.getInstance(shutdownHook())));

        // propagate default size to input plugins
        MessageInput.setDefaultRecvBufferSize(configuration.getUdpRecvBufferSizes());

        // Start services.
        final ServiceManagerListener serviceManagerListener = injector.getInstance(ServiceManagerListener.class);
        serviceManager.addListener(serviceManagerListener);
        try {
            serviceManager.startAsync().awaitHealthy();
        } catch (Exception e) {
            try {
                serviceManager.stopAsync().awaitStopped(configuration.getShutdownTimeout(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException timeoutException) {
                LOG.error("Unable to shutdown properly on time. {}", serviceManager.servicesByState());
            }
            LOG.error("Graylog2 startup failed. Exiting. Exception was:", e);
            System.exit(-1);
        }
        LOG.info("Services started, startup times in ms: {}", serviceManager.startupTimes());

        activityWriter.write(new Activity("Started up.", Main.class));
        LOG.info("Graylog2 " + commandName + " up and running.");

        // Block forever.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            return;
        }
    }

    private String getPluginPath(String configFile) {
        PluginLoaderConfig pluginLoaderConfig = new PluginLoaderConfig();
        JadConfig jadConfig = new JadConfig();
        jadConfig.addConfigurationBean(pluginLoaderConfig);
        jadConfig.setRepositories(getConfigRepositories(configFile));

        try {
            jadConfig.process();
        } catch (RepositoryException e) {
            LOG.error("Couldn't load configuration: {}", e.getMessage());
            System.exit(1);
        } catch (ParameterException | ValidationException e) {
            LOG.error("Invalid configuration", e);
            System.exit(1);
        }

        return pluginLoaderConfig.getPluginDir();
    }

    protected Set<PluginModule> loadPluginModules(String pluginPath) {
        final File pluginDir = new File(pluginPath);
        final Set<PluginModule> pluginModules = new HashSet<>();

        final LegacyPluginLoader legacyPluginLoader = new LegacyPluginLoader(pluginDir);
        for (Plugin plugin : legacyPluginLoader.loadPlugins()) {
            if (version.sameOrHigher(plugin.metadata().getRequiredVersion()))
                pluginModules.addAll(plugin.modules());
            else
                LOG.error("Plugin \"" + plugin.metadata().getName() + "\" requires version " + plugin.metadata().getRequiredVersion() + " - not loading!");
        }

        final PluginLoader pluginLoader = new PluginLoader(pluginDir);
        for (Plugin plugin : pluginLoader.loadPlugins()) {
            if (version.sameOrHigher(plugin.metadata().getRequiredVersion()))
                pluginModules.addAll(plugin.modules());
            else
                LOG.error("Plugin \"" + plugin.metadata().getName() + "\" requires version " + plugin.metadata().getRequiredVersion() + " - not loading!");
        }

        LOG.debug("Loaded modules: " + pluginModules);
        return pluginModules;
    }

    protected List<Module> getSharedBindingsModules(InstantiationService instantiationService) {
        List<Module> result = Lists.newArrayList();
        result.add(new GenericBindings(instantiationService));
        Reflections reflections = new Reflections("org.graylog2.shared.bindings");
        final Set<Class<? extends AbstractModule>> generic = reflections.getSubTypesOf(AbstractModule.class);
        final Set<Class<? extends Graylog2Module>> gl2Modules = reflections.getSubTypesOf(Graylog2Module.class);
        for (Class<? extends Module> type : Iterables.concat(generic, gl2Modules)) {
            // skip the GenericBindings module, because we have already instantiated it above, avoids a bogus log message
            if (type.equals(GenericBindings.class)) {
                continue;
            }
            try {
                Constructor<? extends Module> constructor = type.getConstructor();
                Module module = constructor.newInstance();
                result.add(module);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                LOG.error("Unable to instantiate Module {}: {}", type, e);
            } catch (NoSuchMethodException e) {
                LOG.info("No constructor found for guice module {}", type);
            }
        }
        return result;
    }

    protected Injector setupInjector(NamedConfigParametersModule configModule, Set<PluginModule> pluginModules) {
        try {
            final GuiceInstantiationService instantiationService = new GuiceInstantiationService();

            final ImmutableList.Builder<Module> modules = ImmutableList.builder();
            modules.add(configModule);
            modules.addAll(getSharedBindingsModules(instantiationService));
            modules.addAll(getCommandBindings());
            if (configuration.isMessageJournalEnabled()) {
                modules.add(new KafkaJournalModule());
            } else {
                modules.add(new NoopJournalModule());
            }
            modules.add(new Module() {
                @Override
                public void configure(Binder binder) {
                    binder.bind(String.class).annotatedWith(Names.named("BootstrapCommand")).toInstance(commandName);
                }
            });
            LOG.debug("Adding plugin modules: " + pluginModules);
            modules.addAll(pluginModules);

            final Injector injector = GuiceInjectorHolder.createInjector(modules.build());
            instantiationService.setInjector(injector);

            return injector;
        } catch (CreationException e) {
            annotateInjectorCreationException(e);
            return null;
        } catch (Exception e) {
            LOG.error("Injector creation failed!", e);
            return null;
        }
    }

    protected void savePidFile(final String pidFile) {
        final String pid = Tools.getPID();
        final Path pidFilePath = Paths.get(pidFile);
        pidFilePath.toFile().deleteOnExit();

        try {
            if (pid == null || pid.isEmpty() || pid.equals("unknown")) {
                throw new Exception("Could not determine PID.");
            }

            Files.write(pidFilePath, pid.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.error("Could not write PID file: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    protected Collection<Repository> getConfigRepositories(String configFile) {
        return Arrays.asList(
                new EnvironmentRepository(ENVIRONMENT_PREFIX),
                new SystemPropertiesRepository(PROPERTIES_PREFIX),
                new PropertiesRepository(configFile)
        );
    }

    protected NamedConfigParametersModule readConfiguration(final JadConfig jadConfig, final String configFile) {
        final List<Object> beans = getCommandConfigurationBeans();
        for (Object bean : beans)
            jadConfig.addConfigurationBean(bean);
        jadConfig.setRepositories(getConfigRepositories(configFile));

        LOG.debug("Loading configuration from config file: {}", configFile);
        try {
            jadConfig.process();
        } catch (RepositoryException e) {
            LOG.error("Couldn't load configuration: {}", e.getMessage());
            System.exit(1);
        } catch (ParameterException | ValidationException e) {
            LOG.error("Invalid configuration", e);
            System.exit(1);
        }

        if (configuration.getRestTransportUri() == null) {
            configuration.setRestTransportUri(configuration.getDefaultRestTransportUri());
            LOG.debug("No rest_transport_uri set. Using default [{}].", configuration.getRestTransportUri());
        }

        return new NamedConfigParametersModule(beans);
    }

    private String dumpConfiguration(final Map<String, String> configMap) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Configuration of graylog2-").append(commandName).append(" ").append(version).append(System.lineSeparator());
        sb.append("# Generated on ").append(Tools.iso8601()).append(System.lineSeparator());

        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            sb.append(entry.getKey()).append('=').append(nullToEmpty(entry.getValue())).append(System.lineSeparator());
        }

        return sb.toString();
    }

    protected void annotateProvisionException(ProvisionException e) {
        annotateInjectorExceptions(e.getErrorMessages());
        throw e;
    }

    protected void annotateInjectorCreationException(CreationException e) {
        annotateInjectorExceptions(e.getErrorMessages());
        throw e;
    }

    protected void annotateInjectorExceptions(Collection<Message> messages) {
        for (Message message : messages) {
            if (message.getCause() instanceof NodeIdPersistenceException) {
                LOG.error(UI.wallString("Unable to read or persist your NodeId file. This means your node id file (" + configuration.getNodeIdFile() + ") is not readable or writable by the current user. The following exception might give more information: " + message));
                System.exit(-1);
            } else {
                // other guice error, still print the raw messages
                // TODO this could potentially print duplicate messages depending on what a subclass does...
                LOG.error("Guice error: {}", message.getMessage());
            }
        }
    }
}
