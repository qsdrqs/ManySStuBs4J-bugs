/*
 * Copyright 2012 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.cpr;

import org.atmosphere.config.AtmosphereHandlerConfig;
import org.atmosphere.config.AtmosphereHandlerProperty;
import org.atmosphere.container.BlockingIOCometSupport;
import org.atmosphere.container.JettyWebSocketHandler;
import org.atmosphere.di.InjectorProvider;
import org.atmosphere.di.ServletContextHolder;
import org.atmosphere.di.ServletContextProvider;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;
import org.atmosphere.handler.ReflectorServletProcessor;
import org.atmosphere.util.AtmosphereConfigReader;
import org.atmosphere.util.IntrospectionUtils;
import org.atmosphere.util.Version;
import org.atmosphere.websocket.WebSocket;
import org.atmosphere.websocket.WebSocketProtocol;
import org.atmosphere.websocket.protocol.SimpleHttpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.atmosphere.cpr.ApplicationConfig.ALLOW_QUERYSTRING_AS_REQUEST;
import static org.atmosphere.cpr.ApplicationConfig.ATMOSPHERE_HANDLER;
import static org.atmosphere.cpr.ApplicationConfig.ATMOSPHERE_HANDLER_MAPPING;
import static org.atmosphere.cpr.ApplicationConfig.ATMOSPHERE_HANDLER_PATH;
import static org.atmosphere.cpr.ApplicationConfig.BROADCASTER_CACHE;
import static org.atmosphere.cpr.ApplicationConfig.BROADCASTER_CLASS;
import static org.atmosphere.cpr.ApplicationConfig.BROADCASTER_FACTORY;
import static org.atmosphere.cpr.ApplicationConfig.BROADCASTER_LIFECYCLE_POLICY;
import static org.atmosphere.cpr.ApplicationConfig.BROADCAST_FILTER_CLASSES;
import static org.atmosphere.cpr.ApplicationConfig.DISABLE_ONSTATE_EVENT;
import static org.atmosphere.cpr.ApplicationConfig.PROPERTY_BLOCKING_COMETSUPPORT;
import static org.atmosphere.cpr.ApplicationConfig.PROPERTY_COMET_SUPPORT;
import static org.atmosphere.cpr.ApplicationConfig.PROPERTY_NATIVE_COMETSUPPORT;
import static org.atmosphere.cpr.ApplicationConfig.PROPERTY_SERVLET_MAPPING;
import static org.atmosphere.cpr.ApplicationConfig.PROPERTY_SESSION_SUPPORT;
import static org.atmosphere.cpr.ApplicationConfig.PROPERTY_USE_STREAM;
import static org.atmosphere.cpr.ApplicationConfig.RESUME_AND_KEEPALIVE;
import static org.atmosphere.cpr.ApplicationConfig.WEBSOCKET_PROTOCOL;
import static org.atmosphere.cpr.ApplicationConfig.WEBSOCKET_SUPPORT;
import static org.atmosphere.cpr.FrameworkConfig.ATMOSPHERE_CONFIG;
import static org.atmosphere.cpr.FrameworkConfig.HAZELCAST_BROADCASTER;
import static org.atmosphere.cpr.FrameworkConfig.JERSEY_BROADCASTER;
import static org.atmosphere.cpr.FrameworkConfig.JERSEY_CONTAINER;
import static org.atmosphere.cpr.FrameworkConfig.JGROUPS_BROADCASTER;
import static org.atmosphere.cpr.FrameworkConfig.JMS_BROADCASTER;
import static org.atmosphere.cpr.FrameworkConfig.REDIS_BROADCASTER;
import static org.atmosphere.cpr.FrameworkConfig.WRITE_HEADERS;
import static org.atmosphere.cpr.FrameworkConfig.XMPP_BROADCASTER;
import static org.atmosphere.cpr.HeaderConfig.ATMOSPHERE_POST_BODY;

/**
 * The {@link AtmosphereFramework} acts as a dispatcher for {@link AtmosphereHandler}
 * defined in META-INF/atmosphere.xml, or if atmosphere.xml is missing, all classes
 * that implements {@link AtmosphereHandler} will be discovered and mapped using
 * the class's name.
 * <p/>
 * </pre></blockquote>
 * You can force the framework to use native API of the Web Server instead of
 * the Servlet 3.0 Async API you are deploying on by adding
 * <blockquote><pre>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.useNative&lt;/param-name&gt;
 *      &lt;param-value&gt;true&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </pre></blockquote>
 * You can force this framework to use one Thread per connection instead of
 * native API of the Web Server you are deploying on by adding
 * <blockquote><pre>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.useBlocking&lt;/param-name&gt;
 *      &lt;param-value&gt;true&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </pre></blockquote>
 * You can also define {@link Broadcaster}by adding:
 * <blockquote><pre>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.cpr.broadcasterClass&lt;/param-name&gt;
 *      &lt;param-value&gt;class-name&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </pre></blockquote>
 * You can also for Atmosphere to use {@link java.io.OutputStream} for all write operations.
 * <blockquote><pre>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.useStream&lt;/param-name&gt;
 *      &lt;param-value&gt;true&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </pre></blockquote>
 * You can also configure {@link org.atmosphere.cpr.BroadcasterCache} that persist message when Browser is disconnected.
 * <blockquote><pre>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.cpr.broadcasterCacheClass&lt;/param-name&gt;
 *      &lt;param-value&gt;class-name&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </pre></blockquote>
 * You can also configure Atmosphere to use http session or not
 * <blockquote><pre>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.cpr.sessionSupport&lt;/param-name&gt;
 *      &lt;param-value&gt;false&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </pre></blockquote>
 * You can also configure {@link BroadcastFilter} that will be applied at all newly created {@link Broadcaster}
 * <blockquote><pre>
 *  &lt;init-param&gt;
 *      &lt;param-name&gt;org.atmosphere.cpr.broadcastFilterClasses&lt;/param-name&gt;
 *      &lt;param-value&gt;BroadcastFilter class name separated by coma&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 * </pre></blockquote>
 * All the property available are defined in {@link ApplicationConfig}
 * The Atmosphere Framework can also be used as a Servlet Filter ({@link AtmosphereFilter}).
 * <p/>
 * If you are planning to use JSP, Servlet or JSF, you can instead use the
 * {@link MeteorServlet}, which allow the use of {@link Meteor} inside those
 * components.
 *
 * @author Jeanfrancois Arcand
 */
public class AtmosphereFramework implements ServletContextProvider {

    protected static final Logger logger = LoggerFactory.getLogger(AtmosphereFramework.class);
    protected final List<String> broadcasterFilters = new ArrayList<String>();
    protected final ArrayList<String> possibleComponentsCandidate = new ArrayList<String>();
    protected final HashMap<String, String> initParams = new HashMap<String, String>();
    protected final AtmosphereConfig config;
    protected final AtomicBoolean isCometSupportConfigured = new AtomicBoolean(false);
    protected final boolean isFilter;
    protected final Map<String, AtmosphereHandlerWrapper> atmosphereHandlers = new ConcurrentHashMap<String, AtmosphereHandlerWrapper>();
    protected final ConcurrentLinkedQueue<String> broadcasterTypes = new ConcurrentLinkedQueue<String>();
    protected boolean useNativeImplementation = false;
    protected boolean useBlockingImplementation = false;
    protected boolean useStreamForFlushingComments = false;
    protected CometSupport cometSupport;
    protected String broadcasterClassName = DefaultBroadcaster.class.getName();
    protected boolean isCometSupportSpecified = false;
    protected boolean isBroadcasterSpecified = false;
    protected boolean isSessionSupportSpecified = false;
    protected BroadcasterFactory broadcasterFactory;
    protected String broadcasterFactoryClassName;
    protected static String broadcasterCacheClassName;
    protected boolean webSocketEnabled = true;
    protected String broadcasterLifeCyclePolicy = "NEVER";
    protected String webSocketProtocolClassName = SimpleHttpProtocol.class.getName();
    protected WebSocketProtocol webSocketProtocol;
    protected String handlersPath = "/WEB-INF/classes/";
    protected ServletConfig servletConfig;
    protected boolean autoDetectHandlers = true;

    @Override
    public ServletContext getServletContext() {
        return servletConfig.getServletContext();
    }

    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public List<String> broadcasterFilters() {
        return broadcasterFilters;
    }

    public static final class AtmosphereHandlerWrapper {

        public final AtmosphereHandler atmosphereHandler;
        public Broadcaster broadcaster;
        public String mapping;

        public AtmosphereHandlerWrapper(AtmosphereHandler atmosphereHandler, String mapping) {
            this.atmosphereHandler = atmosphereHandler;
            try {
                if (BroadcasterFactory.getDefault() != null) {
                    this.broadcaster = BroadcasterFactory.getDefault().get(mapping);
                } else {
                    this.mapping = mapping;
                }
            } catch (Exception t) {
                throw new RuntimeException(t);
            }
        }

        public AtmosphereHandlerWrapper(AtmosphereHandler atmosphereHandler, Broadcaster broadcaster) {
            this.atmosphereHandler = atmosphereHandler;
            this.broadcaster = broadcaster;
        }

        @Override
        public String toString() {
            return "AtmosphereHandlerWrapper{ atmosphereHandler=" + atmosphereHandler + ", broadcaster=" +
                    broadcaster + " }";
        }
    }

    /**
     * Return a configured instance of {@link AtmosphereConfig}
     *
     * @return a configured instance of {@link AtmosphereConfig}
     */
    public AtmosphereConfig getAtmosphereConfig() {
        return config;
    }

    /**
     * Simple class/struck that hold the current state.
     */
    public final static class Action {

        public enum TYPE {
            SUSPEND, RESUME, TIMEOUT, CANCELLED, KEEP_ALIVED, CREATED
        }

        public long timeout = -1L;

        public TYPE type;

        public Action() {
            type = TYPE.CREATED;
        }

        public Action(TYPE type) {
            this.type = type;
        }

        public Action(TYPE type, long timeout) {
            this.timeout = timeout;
            this.type = type;
        }
    }

    /**
     * Create an AtmosphereFramework.
     */
    public AtmosphereFramework() {
        this(false, true);
    }

    /**
     * Create an AtmosphereFramework and initialize it via {@link AtmosphereFramework#init(javax.servlet.ServletConfig)}
     */
    public AtmosphereFramework(ServletConfig sc) throws ServletException {
        this(false, true);
        init(sc);
    }

    /**
     * Create an AtmosphereFramework.
     *
     * @param isFilter true if this instance is used as an {@link AtmosphereFilter}
     */
    public AtmosphereFramework(boolean isFilter, boolean autoDetectHandlers) {
        this.isFilter = isFilter;
        this.autoDetectHandlers = autoDetectHandlers;
        readSystemProperties();
        populateBroadcasterType();
        config = new AtmosphereConfig(this);
    }

    /**
     * The order of addition is quite important here.
     */
    private void populateBroadcasterType() {
        broadcasterTypes.add(HAZELCAST_BROADCASTER);
        broadcasterTypes.add(XMPP_BROADCASTER);
        broadcasterTypes.add(REDIS_BROADCASTER);
        broadcasterTypes.add(JGROUPS_BROADCASTER);
        broadcasterTypes.add(JMS_BROADCASTER);
    }

    /**
     * Add an {@link AtmosphereHandler} serviced by the {@link Servlet}
     * This API is exposed to allow embedding an Atmosphere application.
     *
     * @param mapping The servlet mapping (servlet path)
     * @param h       implementation of an {@link AtmosphereHandler}
     */
    public AtmosphereFramework addAtmosphereHandler(String mapping, AtmosphereHandler h) {
        if (!mapping.startsWith("/")) {
            mapping = "/" + mapping;
        }

        AtmosphereHandlerWrapper w = new AtmosphereHandlerWrapper(h, mapping);
        addMapping(mapping, w);
        logger.info("Installed AtmosphereHandler {} mapped to context-path: {}", h.getClass().getName(), mapping);
        return this;
    }

    private AtmosphereFramework addMapping(String path, AtmosphereHandlerWrapper w) {
        // We are using JAXRS mapping algorithm.
        if (path.contains("*")) {
            path = path.replace("*", "[/a-zA-Z0-9-&=;\\?]+");
        }

        atmosphereHandlers.put(path, w);
        return this;
    }

    /**
     * Add an {@link AtmosphereHandler} serviced by the {@link Servlet}
     * This API is exposed to allow embedding an Atmosphere application.
     *
     * @param mapping       The servlet mapping (servlet path)
     * @param h             implementation of an {@link AtmosphereHandler}
     * @param broadcasterId The {@link Broadcaster#getID} value.
     */
    public AtmosphereFramework addAtmosphereHandler(String mapping, AtmosphereHandler h, String broadcasterId) {
        if (!mapping.startsWith("/")) {
            mapping = "/" + mapping;
        }

        AtmosphereHandlerWrapper w = new AtmosphereHandlerWrapper(h, mapping);
        w.broadcaster.setID(broadcasterId);
        addMapping(mapping, w);
        logger.info("Installed AtmosphereHandler {} mapped to context-path: {}", h.getClass().getName(), mapping);
        return this;
    }

    /**
     * Add an {@link AtmosphereHandler} serviced by the {@link Servlet}
     * This API is exposed to allow embedding an Atmosphere application.
     *
     * @param mapping     The servlet mapping (servlet path)
     * @param h           implementation of an {@link AtmosphereHandler}
     * @param broadcaster The {@link Broadcaster} associated with AtmosphereHandler.
     */
    public AtmosphereFramework addAtmosphereHandler(String mapping, AtmosphereHandler h, Broadcaster broadcaster) {
        if (!mapping.startsWith("/")) {
            mapping = "/" + mapping;
        }

        AtmosphereHandlerWrapper w = new AtmosphereHandlerWrapper(h, broadcaster);
        addMapping(mapping, w);
        logger.info("Installed AtmosphereHandler {} mapped to context-path: {}", h.getClass().getName(), mapping);
        return this;
    }

    /**
     * Remove an {@link AtmosphereHandler}
     *
     * @param mapping the mapping used when invoking {@link #addAtmosphereHandler(String, AtmosphereHandler)};
     * @return true if removed
     */
    public AtmosphereFramework removeAtmosphereHandler(String mapping) {
        atmosphereHandlers.remove(mapping);
        return this;
    }

    /**
     * Remove all {@link AtmosphereHandler}
     */
    public AtmosphereFramework removeAllAtmosphereHandler() {
        atmosphereHandlers.clear();
        return this;
    }

    /**
     * Remove all init parameters.
     */
    public AtmosphereFramework removeAllInitParams() {
        initParams.clear();
        return this;
    }

    /**
     * Add init-param like if they were defined in web.xml
     *
     * @param name  The name
     * @param value The value
     */
    public AtmosphereFramework addInitParameter(String name, String value) {
        initParams.put(name, value);
        return this;
    }

    protected void readSystemProperties() {
        if (System.getProperty(PROPERTY_NATIVE_COMETSUPPORT) != null) {
            useNativeImplementation = Boolean
                    .parseBoolean(System.getProperty(PROPERTY_NATIVE_COMETSUPPORT));
            isCometSupportSpecified = true;
        }

        if (System.getProperty(PROPERTY_BLOCKING_COMETSUPPORT) != null) {
            useBlockingImplementation = Boolean
                    .parseBoolean(System.getProperty(PROPERTY_BLOCKING_COMETSUPPORT));
            isCometSupportSpecified = true;
        }

        if (System.getProperty(DISABLE_ONSTATE_EVENT) != null) {
            initParams.put(DISABLE_ONSTATE_EVENT, System.getProperty(DISABLE_ONSTATE_EVENT));
        }
    }

    /**
     * Load the {@link AtmosphereHandler} associated with this AtmosphereServlet.
     *
     * @param sc the {@link ServletContext}
     */
    public AtmosphereFramework init(final ServletConfig sc) throws ServletException {
        try {
            ServletContextHolder.register(this);

            ServletConfig scFacade = new ServletConfig() {

                public String getServletName() {
                    return sc.getServletName();
                }

                public ServletContext getServletContext() {
                    return sc.getServletContext();
                }

                public String getInitParameter(String name) {
                    String param = sc.getInitParameter(name);
                    if (param == null) {
                        return initParams.get(name);
                    }
                    return param;
                }

                public Enumeration<String> getInitParameterNames() {
                    Enumeration en = sc.getInitParameterNames();
                    while (en.hasMoreElements()) {
                        String name = (String) en.nextElement();
                        if (!initParams.containsKey(name)) {
                            initParams.put(name, name);
                        }
                    }
                    return Collections.enumeration(initParams.keySet());
                }
            };
            this.servletConfig = scFacade;
            doInitParams(scFacade);
            doInitParamsForWebSocket(scFacade);
            configureBroadcaster(sc.getServletContext());
            loadConfiguration(scFacade);

            autoDetectContainer();
            configureWebDotXmlAtmosphereHandler(sc);
            initWebSocketProtocol();
            cometSupport.init(scFacade);
            initAtmosphereHandler(scFacade);

            logger.info("Using Broadcaster class: {}", broadcasterClassName);
            logger.info("Atmosphere Framework {} started.", Version.getRawVersion());
        } catch (Throwable t) {
            logger.error("Failed to initialize Atmosphere Framework", t);

            if (t instanceof ServletException) {
                throw (ServletException) t;
            }

            throw new ServletException(t);
        }
        return this;
    }

    protected void configureWebDotXmlAtmosphereHandler(ServletConfig sc) {
        String s = sc.getInitParameter(ATMOSPHERE_HANDLER);
        if (s != null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {

                String mapping = sc.getInitParameter(ATMOSPHERE_HANDLER_MAPPING);
                if (mapping == null) {
                    mapping = "/*";
                }
                addAtmosphereHandler(mapping, (AtmosphereHandler) cl.loadClass(s).newInstance());
            } catch (Exception ex) {
                logger.warn("Unable to load WebSocketHandle instance", ex);
            }
        }
    }

    protected void configureBroadcaster(ServletContext sc) throws ClassNotFoundException, InstantiationException, IllegalAccessException {

        if (broadcasterFactoryClassName != null) {
            broadcasterFactory = (BroadcasterFactory) Thread.currentThread().getContextClassLoader()
                    .loadClass(broadcasterFactoryClassName).newInstance();
        }

        if (broadcasterFactory == null) {
            Class<? extends Broadcaster> bc =
                    (Class<? extends Broadcaster>) Thread.currentThread().getContextClassLoader()
                            .loadClass(broadcasterClassName);

            logger.info("Using BroadcasterFactory class: {}", DefaultBroadcasterFactory.class.getName());

            broadcasterFactory = new DefaultBroadcasterFactory(bc, broadcasterLifeCyclePolicy, config);
        }

        // http://java.net/jira/browse/ATMOSPHERE-157
        if (sc != null) {
            sc.setAttribute(BroadcasterFactory.class.getName(), broadcasterFactory);
        }

        BroadcasterFactory.setBroadcasterFactory(broadcasterFactory, config);
        InjectorProvider.getInjector().inject(broadcasterFactory);

        Iterator<Entry<String, AtmosphereHandlerWrapper>> i = atmosphereHandlers.entrySet().iterator();
        AtmosphereHandlerWrapper w;
        Entry<String, AtmosphereHandlerWrapper> e;
        while (i.hasNext()) {
            e = i.next();
            w = e.getValue();
            BroadcasterConfig broadcasterConfig = new BroadcasterConfig(broadcasterFilters, config);

            if (w.broadcaster == null) {
                w.broadcaster = broadcasterFactory.get(w.mapping);
            } else {
                w.broadcaster.setBroadcasterConfig(broadcasterConfig);
                if (broadcasterCacheClassName != null) {
                    BroadcasterCache cache = (BroadcasterCache) Thread.currentThread().getContextClassLoader()
                            .loadClass(broadcasterCacheClassName).newInstance();
                    InjectorProvider.getInjector().inject(cache);
                    broadcasterConfig.setBroadcasterCache(cache);
                }
            }
        }
    }

    protected void doInitParamsForWebSocket(ServletConfig sc) {
        String s = sc.getInitParameter(WEBSOCKET_SUPPORT);
        if (s != null) {
            webSocketEnabled = Boolean.parseBoolean(s);
            sessionSupport(false);
        }
        s = sc.getInitParameter(WEBSOCKET_PROTOCOL);
        if (s != null) {
            webSocketProtocolClassName = s;
        }
    }

    /**
     * Read init param from web.xml and apply them.
     *
     * @param sc {@link ServletConfig}
     */
    protected void doInitParams(ServletConfig sc) {
        String s = sc.getInitParameter(PROPERTY_NATIVE_COMETSUPPORT);
        if (s != null) {
            useNativeImplementation = Boolean.parseBoolean(s);
            if (useNativeImplementation) isCometSupportSpecified = true;
        }
        s = sc.getInitParameter(PROPERTY_BLOCKING_COMETSUPPORT);
        if (s != null) {
            useBlockingImplementation = Boolean.parseBoolean(s);
            if (useBlockingImplementation) isCometSupportSpecified = true;
        }
        s = sc.getInitParameter(PROPERTY_USE_STREAM);
        if (s != null) {
            useStreamForFlushingComments = Boolean.parseBoolean(s);
        }
        s = sc.getInitParameter(PROPERTY_COMET_SUPPORT);
        if (s != null) {
            cometSupport = new DefaultCometSupportResolver(config).newCometSupport(s);
            isCometSupportSpecified = true;
        }
        s = sc.getInitParameter(BROADCASTER_CLASS);
        if (s != null) {
            broadcasterClassName = s;
            isBroadcasterSpecified = true;
        }
        s = sc.getInitParameter(BROADCASTER_CACHE);
        if (s != null) {
            broadcasterCacheClassName = s;
        }
        s = sc.getInitParameter(PROPERTY_SESSION_SUPPORT);
        if (s != null) {
            config.setSupportSession(Boolean.valueOf(s));
            isSessionSupportSpecified = true;
        }
        s = sc.getInitParameter(DISABLE_ONSTATE_EVENT);
        if (s != null) {
            initParams.put(DISABLE_ONSTATE_EVENT, s);
        } else {
            initParams.put(DISABLE_ONSTATE_EVENT, "false");
        }
        s = sc.getInitParameter(RESUME_AND_KEEPALIVE);
        if (s != null) {
            initParams.put(RESUME_AND_KEEPALIVE, s);
        }
        s = sc.getInitParameter(BROADCAST_FILTER_CLASSES);
        if (s != null) {
            broadcasterFilters.addAll(Arrays.asList(s.split(",")));
        }
        s = sc.getInitParameter(BROADCASTER_LIFECYCLE_POLICY);
        if (s != null) {
            broadcasterLifeCyclePolicy = s;
        }
        s = sc.getInitParameter(BROADCASTER_FACTORY);
        if (s != null) {
            broadcasterFactoryClassName = s;
        }
        s = sc.getInitParameter(ATMOSPHERE_HANDLER_PATH);
        if (s != null) {
            handlersPath = s;
        }
    }

    public void loadConfiguration(ServletConfig sc) throws ServletException {

        if (!autoDetectHandlers) return;

        try {
            URL url = sc.getServletContext().getResource(handlersPath);
            URLClassLoader urlC = new URLClassLoader(new URL[]{url},
                    Thread.currentThread().getContextClassLoader());
            loadAtmosphereDotXml(sc.getServletContext().
                    getResourceAsStream("/META-INF/atmosphere.xml"), urlC);

            if (atmosphereHandlers.size() == 0) {
                autoDetectAtmosphereHandlers(sc.getServletContext(), urlC);

                if (atmosphereHandlers.size() == 0) {
                    detectSupportedFramework(sc);
                }
            }

            autoDetectWebSocketHandler(sc.getServletContext(), urlC);
        } catch (Throwable t) {
            throw new ServletException(t);
        }
    }

    /**
     * Auto-detect Jersey when no atmosphere.xml file are specified.
     *
     * @param sc {@link ServletConfig}
     * @return true if Jersey classes are detected
     * @throws ClassNotFoundException
     */
    protected boolean detectSupportedFramework(ServletConfig sc) throws ClassNotFoundException, IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String broadcasterClassNameTmp = null;

        try {
            cl.loadClass(JERSEY_CONTAINER);

            if (!isBroadcasterSpecified) {
                broadcasterClassNameTmp = lookupDefaultBroadcasterType();

                cl.loadClass(broadcasterClassNameTmp);
            }
            useStreamForFlushingComments = true;
        } catch (Throwable t) {
            logger.trace("", t);
            return false;
        }

        logger.warn("Missing META-INF/atmosphere.xml but found the Jersey runtime. Starting Jersey");

        // Jersey will handle itself the headers.
        initParams.put(WRITE_HEADERS, "false");

        ReflectorServletProcessor rsp = new ReflectorServletProcessor();
        if (broadcasterClassNameTmp != null) broadcasterClassName = broadcasterClassNameTmp;
        rsp.setServletClassName(JERSEY_CONTAINER);
        sessionSupport(false);
        initParams.put(DISABLE_ONSTATE_EVENT, "true");

        String mapping = sc.getInitParameter(PROPERTY_SERVLET_MAPPING);
        if (mapping == null) {
            mapping = "/*";
        }
        Class<? extends Broadcaster> bc = (Class<? extends Broadcaster>) cl.loadClass(broadcasterClassName);

        broadcasterFactory.destroy();
        logger.info("Using BroadcasterFactory class: {}", DefaultBroadcasterFactory.class.getName());

        broadcasterFactory = new DefaultBroadcasterFactory(bc, broadcasterLifeCyclePolicy, config);
        Broadcaster b = BroadcasterFactory.getDefault().get(bc, mapping);

        addAtmosphereHandler(mapping, rsp, b);
        return true;
    }

    protected String lookupDefaultBroadcasterType() {
        for (String b : broadcasterTypes) {
            try {
                Class.forName(b);
                return b;
            } catch (ClassNotFoundException e) {
            }
        }
        return JERSEY_BROADCASTER;
    }

    protected void sessionSupport(boolean sessionSupport) {
        if (!isSessionSupportSpecified) {
            config.setSupportSession(sessionSupport);
        }
    }

    /**
     * Initialize {@link AtmosphereServletProcessor}
     *
     * @param sc the {@link ServletConfig}
     * @throws javax.servlet.ServletException
     */
    void initAtmosphereHandler(ServletConfig sc) throws ServletException {
        AtmosphereHandler a;
        AtmosphereHandlerWrapper w;
        for (Entry<String, AtmosphereHandlerWrapper> h : atmosphereHandlers.entrySet()) {
            w = h.getValue();
            a = w.atmosphereHandler;
            if (a instanceof AtmosphereServletProcessor) {
                ((AtmosphereServletProcessor) a).init(sc);
            }
        }

        if (atmosphereHandlers.size() == 0 && !SimpleHttpProtocol.class.isAssignableFrom(webSocketProtocol.getClass())) {
            logger.debug("Adding a void AtmosphereHandler mapped to /* to allow WebSocket application only");
            addAtmosphereHandler("/*", new AbstractReflectorAtmosphereHandler() {
                @Override
                public void onRequest(AtmosphereResource httpServletRequestHttpServletResponseAtmosphereResource) throws IOException {
                }

                @Override
                public void destroy() {
                }
            });
        }
    }

    protected void initWebSocketProtocol() {
        if (webSocketProtocol != null) return;
        try {
            webSocketProtocol = (WebSocketProtocol) AtmosphereFramework.class.getClassLoader()
                    .loadClass(webSocketProtocolClassName).newInstance();
            logger.info("Installed WebSocketProtocol {} ", webSocketProtocolClassName );
        } catch (Exception ex) {
            logger.error("Cannot load the WebSocketProtocol {}", getWebSocketProtocolClassName(), ex);
            webSocketProtocol = new SimpleHttpProtocol();
        }
        webSocketProtocol.configure(config);
    }

    public AtmosphereFramework destroy() {
        if (cometSupport != null && AsynchronousProcessor.class.isAssignableFrom(cometSupport.getClass())) {
            ((AsynchronousProcessor) cometSupport).shutdown();
        }

        // We just need one bc to shutdown the shared thread pool
        BroadcasterConfig bc = null;
        for (Entry<String, AtmosphereHandlerWrapper> entry : atmosphereHandlers.entrySet()) {
            AtmosphereHandlerWrapper handlerWrapper = entry.getValue();
            handlerWrapper.atmosphereHandler.destroy();
        }

        BroadcasterFactory factory = BroadcasterFactory.getDefault();
        if (factory != null) {
            factory.destroy();
            BroadcasterFactory.factory = null;
        }
        return this;
    }

    /**
     * Load AtmosphereHandler defined under META-INF/atmosphere.xml
     *
     * @param stream The input stream we read from.
     * @param c      The classloader
     */
    protected void loadAtmosphereDotXml(InputStream stream, URLClassLoader c)
            throws IOException, ServletException {

        if (stream == null) {
            return;
        }

        AtmosphereConfigReader.getInstance().parse(config, stream);
        for (AtmosphereHandlerConfig atmoHandler : config.getAtmosphereHandler()) {
            try {
                AtmosphereHandler handler;

                if (!ReflectorServletProcessor.class.getName().equals(atmoHandler.getClassName())) {
                    handler = (AtmosphereHandler) c.loadClass(atmoHandler.getClassName()).newInstance();
                    InjectorProvider.getInjector().inject(handler);
                } else {
                    handler = new ReflectorServletProcessor();
                }

                logger.info("Installed AtmosphereHandler {} mapped to context-path: {}", handler, atmoHandler.getContextRoot());

                boolean isJersey = false;
                for (AtmosphereHandlerProperty handlerProperty : atmoHandler.getProperties()) {

                    if (handlerProperty.getName() != null && handlerProperty.getName().indexOf("jersey") != -1) {
                        isJersey = true;
                        initParams.put(DISABLE_ONSTATE_EVENT, "true");
                        useStreamForFlushingComments = true;
                        broadcasterClassName = lookupDefaultBroadcasterType();
                    }

                    IntrospectionUtils.setProperty(handler, handlerProperty.getName(), handlerProperty.getValue());
                    IntrospectionUtils.addProperty(handler, handlerProperty.getName(), handlerProperty.getValue());
                }

                config.setSupportSession(!isJersey);

                if (!atmoHandler.getSupportSession().equals("")) {
                    sessionSupport(Boolean.valueOf(atmoHandler.getSupportSession()));
                }

                String broadcasterClass = atmoHandler.getBroadcaster();
                Broadcaster b;
                /**
                 * If there is more than one AtmosphereHandler defined, their Broadcaster
                 * may clash each other with the BroadcasterFactory. In that case we will use the
                 * last one defined.
                 */
                if (broadcasterClass != null) {
                    broadcasterClassName = broadcasterClass;
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Class<? extends Broadcaster> bc = (Class<? extends Broadcaster>) cl.loadClass(broadcasterClassName);
                    broadcasterFactory = new DefaultBroadcasterFactory(bc, broadcasterLifeCyclePolicy, config);
                    BroadcasterFactory.setBroadcasterFactory(broadcasterFactory, config);
                }

                b = BroadcasterFactory.getDefault().get(atmoHandler.getContextRoot());

                AtmosphereHandlerWrapper wrapper = new AtmosphereHandlerWrapper(handler, b);
                addMapping(atmoHandler.getContextRoot(), wrapper);

                String bc = atmoHandler.getBroadcasterCache();
                if (bc != null) {
                    broadcasterCacheClassName = bc;
                }

                if (atmoHandler.getCometSupport() != null) {
                    cometSupport = (CometSupport) c.loadClass(atmoHandler.getCometSupport())
                            .getDeclaredConstructor(new Class[]{AtmosphereConfig.class})
                            .newInstance(new Object[]{config});
                }

                if (atmoHandler.getBroadcastFilterClasses() != null) {
                    broadcasterFilters.addAll(atmoHandler.getBroadcastFilterClasses());
                }

            } catch (Throwable t) {
                logger.warn("unable to load AtmosphereHandler class: " + atmoHandler.getClassName(), t);
                throw new ServletException(t);
            }

        }
    }

    /**
     * Set the {@link CometSupport} implementation. Make sure you don't set
     * an implementation that only works on some Container. See {@link BlockingIOCometSupport}
     * for an example.
     *
     * @param cometSupport
     */
    public AtmosphereFramework setCometSupport(CometSupport cometSupport) {
        this.cometSupport = cometSupport;
        return this;
    }

    /**
     * Return the current {@link CometSupport}
     *
     * @return the current {@link CometSupport}
     */
    public CometSupport getCometSupport() {
        return cometSupport;
    }

    /**
     * Returns an instance of CometSupportResolver {@link CometSupportResolver}
     *
     * @return CometSupportResolver
     */
    protected CometSupportResolver createCometSupportResolver() {
        return new DefaultCometSupportResolver(config);
    }


    /**
     * Auto detect the underlying Servlet Container we are running on.
     */
    protected void autoDetectContainer() {
        // Was defined in atmosphere.xml
        if (getCometSupport() == null) {
            setCometSupport(createCometSupportResolver()
                    .resolve(useNativeImplementation, useBlockingImplementation, webSocketEnabled));
        }

        logger.info("Atmosphere is using async support: {} running under container: {}",
                getCometSupport().getClass().getName(), cometSupport.getContainerName());
    }

    /**
     * Auto detect instance of {@link AtmosphereHandler} in case META-INF/atmosphere.xml
     * is missing.
     *
     * @param servletContext {@link ServletContext}
     * @param classloader    {@link URLClassLoader} to load the class.
     * @throws java.net.MalformedURLException
     * @throws java.net.URISyntaxException
     */
    public void autoDetectAtmosphereHandlers(ServletContext servletContext, URLClassLoader classloader)
            throws MalformedURLException, URISyntaxException {

        // If Handler has been added
        if (atmosphereHandlers.size() > 0) return;

        logger.info("Auto detecting atmosphere handlers {}", handlersPath);

        String realPath = servletContext.getRealPath(handlersPath);

        // Weblogic bug
        if (realPath == null) {
            URL u = servletContext.getResource(handlersPath);
            if (u == null) return;
            realPath = u.getPath();
        }

        loadAtmosphereHandlersFromPath(classloader, realPath);
    }

    public void loadAtmosphereHandlersFromPath(URLClassLoader classloader, String realPath) {
        File file = new File(realPath);

        if (file.isDirectory()) {
            getFiles(file);

            for (String className : possibleComponentsCandidate) {
                try {
                    className = className.replace('\\', '/');
                    className = className.replaceFirst("^.*/(WEB-INF|target)/(test-)?classes/(.*)\\.class", "$3").replace("/", ".");
                    Class<?> clazz = classloader.loadClass(className);

                    if (AtmosphereHandler.class.isAssignableFrom(clazz)) {
                        AtmosphereHandler handler = (AtmosphereHandler) clazz.newInstance();
                        InjectorProvider.getInjector().inject(handler);
                        addMapping("/" + handler.getClass().getSimpleName(),
                                new AtmosphereHandlerWrapper(handler, "/" + handler.getClass().getSimpleName()));
                        logger.info("Installed AtmosphereHandler {} mapped to context-path: {}", handler, handler.getClass().getName());
                    }
                } catch (Throwable t) {
                    logger.trace("failed to load class as an AtmosphereHandler: " + className, t);
                }
            }
        }
    }

    /**
     * Auto detect instance of {@link org.atmosphere.websocket.WebSocketHandler} in case META-INF/atmosphere.xml
     * is missing.
     *
     * @param servletContext {@link ServletContext}
     * @param classloader    {@link URLClassLoader} to load the class.
     * @throws java.net.MalformedURLException
     * @throws java.net.URISyntaxException
     */
    protected void autoDetectWebSocketHandler(ServletContext servletContext, URLClassLoader classloader)
            throws MalformedURLException, URISyntaxException {


        if (webSocketProtocolClassName.equalsIgnoreCase(SimpleHttpProtocol.class.getName())) return;

        logger.info("Auto detecting WebSocketHandler {}", handlersPath);

        String realPath = servletContext.getRealPath(handlersPath);

        // Weblogic bug
        if (realPath == null) {
            URL u = servletContext.getResource(handlersPath);
            if (u == null) return;
            realPath = u.getPath();
        }

        loadWebSocketFromPath(classloader, realPath);
    }

    protected void loadWebSocketFromPath(URLClassLoader classloader, String realPath) {
        File file = new File(realPath);

        if (file.isDirectory()) {
            getFiles(file);

            for (String className : possibleComponentsCandidate) {
                try {
                    className = className.replace('\\', '/');
                    className = className.replaceFirst("^.*/(WEB-INF|target)/(test-)?classes/(.*)\\.class", "$3").replace("/", ".");
                    Class<?> clazz = classloader.loadClass(className);

                    if (WebSocketProtocol.class.isAssignableFrom(clazz)) {
                        webSocketProtocol = (WebSocketProtocol) clazz.newInstance();
                        InjectorProvider.getInjector().inject(webSocketProtocol);
                        logger.info("Installed WebSocketProtocol {}", webSocketProtocol);
                    }
                } catch (Throwable t) {
                    logger.trace("failed to load class as an WebSocketProtocol: " + className, t);
                }
            }
        }
    }


    /**
     * Get the list of possible candidate to load as {@link AtmosphereHandler}
     *
     * @param f the real path {@link File}
     */
    private void getFiles(File f) {

        if (possibleComponentsCandidate.size() > 0) return; // We already scanned.

        File[] files = f.listFiles();
        for (File test : files) {
            if (test.isDirectory()) {
                getFiles(test);
            } else {
                String clazz = test.getAbsolutePath();
                if (clazz.endsWith(".class")) {
                    possibleComponentsCandidate.add(clazz);
                }
            }
        }
    }

    /**
     * Invoke the proprietary {@link CometSupport}
     *
     * @param req
     * @param res
     * @return an {@link Action}
     * @throws IOException
     * @throws ServletException
     */
    public Action doCometSupport(AtmosphereRequest req, AtmosphereResponse res) throws IOException, ServletException {
        req.setAttribute(BROADCASTER_FACTORY, broadcasterFactory);
        req.setAttribute(PROPERTY_USE_STREAM, useStreamForFlushingComments);
        req.setAttribute(BROADCASTER_CLASS, broadcasterClassName);
        req.setAttribute(ATMOSPHERE_CONFIG, config);

        Action a = null;
        try {
            if ((config.getInitParameter(ALLOW_QUERYSTRING_AS_REQUEST) != null
                    || (isIECandidate(req) || req.getParameter(HeaderConfig.JSONP_CALLBACK_NAME) != null))
                    && req.getAttribute(WebSocket.WEBSOCKET_SUSPEND) == null) {

                Map<String, String> headers = configureQueryStringAsRequest(req);
                String body = headers.remove(ATMOSPHERE_POST_BODY);
                if (body != null && body.isEmpty()) {
                    body = null;
                }

                req.headers(headers)
                   .method(body != null && req.getMethod().equalsIgnoreCase("GET") ? "POST" : req.getMethod());

                if (body != null) {
                   req.body(body);
                }

                a = cometSupport.service(req, res);
            } else {
                return cometSupport.service(req, res);
            }
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().startsWith("Tomcat failed")) {
                if (!isFilter) {
                    logger.warn("Failed using comet support: {}, error: {} Is the Nio or Apr Connector enabled?", cometSupport.getClass().getName(),
                            ex.getMessage());
                    logger.warn("Using BlockingIOCometSupport.");
                }
                logger.trace(ex.getMessage(), ex);

                cometSupport = new BlockingIOCometSupport(config);
                doCometSupport(req, res);
            } else {
                logger.error("AtmosphereServlet exception", ex);
                throw ex;
            }
        } finally {
            if (req != null && a != null && a.type != Action.TYPE.SUSPEND) {
                req.destroy();
                res.destroy();
            }
        }
        return null;
    }

    /**
     * Return the default {@link Broadcaster} class name.
     *
     * @return the broadcasterClassName
     */
    public String getDefaultBroadcasterClassName() {
        return broadcasterClassName;
    }

    /**
     * Set the default {@link Broadcaster} class name
     *
     * @param bccn the broadcasterClassName to set
     */
    public AtmosphereFramework setDefaultBroadcasterClassName(String bccn) {
        broadcasterClassName = bccn;
        return this;
    }

    /**
     * <tt>true</tt> if Atmosphere uses {@link AtmosphereResponse#getOutputStream()}
     * by default for write operation.
     *
     * @return the useStreamForFlushingComments
     */
    public boolean isUseStreamForFlushingComments() {
        return useStreamForFlushingComments;
    }

    /**
     * Set to <tt>true</tt> so Atmosphere uses {@link AtmosphereResponse#getOutputStream()}
     * by default for write operation. Default is false.
     *
     * @param useStreamForFlushingComments the useStreamForFlushingComments to set
     */
    public AtmosphereFramework setUseStreamForFlushingComments(boolean useStreamForFlushingComments) {
        this.useStreamForFlushingComments = useStreamForFlushingComments;
        return this;
    }

    /**
     * Get the {@link BroadcasterFactory} which is used by Atmosphere to construct
     * {@link Broadcaster}
     *
     * @return {@link BroadcasterFactory}
     */
    public BroadcasterFactory getBroadcasterFactory() {
        return broadcasterFactory;
    }

    /**
     * Set the {@link BroadcasterFactory} which is used by Atmosphere to construct
     * {@link Broadcaster}
     *
     * @return {@link BroadcasterFactory}
     */
    public AtmosphereFramework setBroadcasterFactory(final BroadcasterFactory broadcasterFactory) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this.broadcasterFactory = broadcasterFactory;
        configureBroadcaster(config.getServletContext());
        return this;
    }

    /**
     * Return the {@link org.atmosphere.cpr.BroadcasterCache} class name.
     *
     * @return the {@link org.atmosphere.cpr.BroadcasterCache} class name.
     */
    public String getBroadcasterCacheClassName() {
        return broadcasterCacheClassName;
    }

    /**
     * Set the {@link org.atmosphere.cpr.BroadcasterCache} class name.
     *
     * @param broadcasterCacheClassName
     */
    public void setBroadcasterCacheClassName(String broadcasterCacheClassName) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        this.broadcasterCacheClassName = broadcasterCacheClassName;
        configureBroadcaster(config.getServletContext());
    }

    /**
     * Add a new Broadcaster class name AtmosphereServlet can use when initializing requests, and when
     * atmosphere.xml broadcaster element is unspecified.
     *
     * @param broadcasterTypeString
     */
    public AtmosphereFramework addBroadcasterType(String broadcasterTypeString) {
        broadcasterTypes.add(broadcasterTypeString);
        return this;
    }

    public String getWebSocketProtocolClassName() {
        return webSocketProtocolClassName;
    }

    public AtmosphereFramework setWebSocketProtocolClassName(String webSocketProtocolClassName) {
        this.webSocketProtocolClassName = webSocketProtocolClassName;
        return this;
    }

    public Map<String, AtmosphereHandlerWrapper> getAtmosphereHandlers() {
        return atmosphereHandlers;
    }

    protected Map<String, String> configureQueryStringAsRequest(AtmosphereRequest request) {
        Map<String, String> headers = new HashMap<String, String>();

        Enumeration<String> e = request.getParameterNames();
        String s;
        while (e.hasMoreElements()) {
            s = e.nextElement();
            headers.put(s, request.getParameter(s));
        }
        return headers;
    }

    protected boolean isIECandidate(AtmosphereRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return false;

        if (userAgent.contains("MSIE") || userAgent.contains(".NET")) {
            // Now check the header
            String transport = request.getHeader(HeaderConfig.X_ATMOSPHERE_TRANSPORT);
            if (transport != null) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public WebSocketProtocol getWebSocketProtocol() {
        return webSocketProtocol;
    }
}
