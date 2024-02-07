/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cli.command;

import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import joptsimple.OptionSet;

import org.springframework.boot.cli.Command;
import org.springframework.boot.cli.CommandFactory;
import org.springframework.boot.cli.SpringCli;
import org.springframework.boot.cli.compiler.GroovyCompiler;
import org.springframework.boot.cli.compiler.GroovyCompilerConfiguration;
import org.springframework.boot.cli.compiler.GroovyCompilerConfigurationAdapter;
import org.springframework.boot.cli.compiler.GroovyCompilerScope;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;

/**
 * <p>
 * Command to initialize the Spring CLI with commands from the classpath. If the current
 * context class loader is a GroovyClassLoader then it can be enhanced by passing in
 * compiler options (e.g. <code>--classpath=...</code>).
 * </p>
 * <p>
 * If the current context class loader is not already GroovyClassLoader then one will be
 * created and will replace the current context loader. In this case command arguments can
 * include files to compile that have <code>@Grab</code> annotations to process. By
 * default a script called "init.groovy" or "spring.groovy" is used if it exists in the
 * current directory or the root of the classpath.
 * </p>
 * 
 * @author Dave Syer
 */
public class InitCommand extends OptionParsingCommand {

	public static final String NAME = "init";

	public InitCommand(SpringCli cli) {
		super(NAME, "(Re)-initialize the Spring cli", new InitOptionHandler(cli));
	}

	private static class InitOptionHandler extends CompilerOptionHandler {

		private SpringCli cli;
		private GroovyCompiler compiler;

		public InitOptionHandler(SpringCli cli) {
			this.cli = cli;
		}

		@Override
		protected void run(OptionSet options) throws Exception {

			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			boolean enhanced = false;

			SourceOptions sourceOptions = new SourceOptions(options, loader,
					"init.groovy", "spring.groovy");
			String[] sources = sourceOptions.getSourcesArray();

			if (!(loader instanceof GroovyClassLoader) && sources.length > 0) {

				List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
						.createDefaultRepositoryConfiguration();

				GroovyCompilerConfiguration configuration = new InitGroovyCompilerConfigurationAdapter(
						options, this, repositoryConfiguration);

				this.compiler = new GroovyCompiler(configuration);
				this.compiler
						.addCompilationCustomizers(new ScriptCompilationCustomizer());
				loader = this.compiler.getLoader();
				Thread.currentThread().setContextClassLoader(loader);

			}
			else {
				String classpath = getClasspathOption().value(options);
				if (classpath != null && classpath.length() > 0) {
					((GroovyClassLoader) loader).addClasspath(classpath);
					enhanced = true;
				}
			}

			if (this.compiler != null && sources.length > 0) {
				Class<?>[] classes = this.compiler.compile(sources);
				for (Class<?> type : classes) {
					Command script = ScriptCommand.command(type);
					if (script != null) {
						this.cli.register(script);
					}
					else if (CommandFactory.class.isAssignableFrom(type)) {
						for (Command command : ((CommandFactory) type.newInstance())
								.getCommands(this.cli)) {
							this.cli.register(command);
						}
					}
					else if (Commands.class.isAssignableFrom(type)) {
						Commands instance = (Commands) type.newInstance();
						Map<String, Closure<?>> commands = instance.getCommands();
						Map<String, OptionHandler> handlers = instance.getOptions();
						for (String command : commands.keySet()) {
							if (handlers.containsKey(command)) {
								// An OptionHandler is available
								OptionHandler handler = handlers.get(command);
								handler.setClosure(commands.get(command));
								this.cli.register(new ScriptCommand(command, handler));
							}
							else {
								// Otherwise just a plain Closure
								this.cli.register(new ScriptCommand(command, commands
										.get(command)));

							}
						}
					}
					else if (Script.class.isAssignableFrom(type)) {
						((Script) type.newInstance()).run();
					}
				}
				enhanced = true;
			}

			if (this.cli.getCommands().isEmpty() || enhanced) {

				for (CommandFactory factory : ServiceLoader.load(CommandFactory.class,
						loader)) {
					for (Command command : factory.getCommands(this.cli)) {
						this.cli.register(command);
					}
				}

			}

		}
	}

	private static class InitGroovyCompilerConfigurationAdapter extends
			GroovyCompilerConfigurationAdapter {
		private InitGroovyCompilerConfigurationAdapter(OptionSet optionSet,
				CompilerOptionHandler compilerOptionHandler,
				List<RepositoryConfiguration> repositoryConfiguration) {
			super(optionSet, compilerOptionHandler, repositoryConfiguration);
		}

		@Override
		public GroovyCompilerScope getScope() {
			return GroovyCompilerScope.EXTENSION;
		}
	}

	public static interface Commands {
		Map<String, Closure<?>> getCommands();

		Map<String, OptionHandler> getOptions();
	}

}
