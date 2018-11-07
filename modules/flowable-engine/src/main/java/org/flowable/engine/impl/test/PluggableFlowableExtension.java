/* Licensed under the Apache License, Version 2.0 (the "License");
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
package org.flowable.engine.impl.test;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.cfg.CommandExecutorImpl;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.impl.interceptor.CommandInvoker;
import org.flowable.engine.impl.interceptor.LoggingExecutionTreeCommandInvoker;
import org.flowable.engine.test.EnableVerboseExecutionTreeLogging;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * An extension which uses the {@link ProcessEngines#getDefaultProcessEngine()} and is cached within the entire context
 * (i.e. it would be reused by all users of the extension).
 * <p>
 * This extension also activates {@link EnableVerboseExecutionTreeLogging} if a class is annotated with it.
 *
 * @author Filip Hrisafov
 */
public class PluggableFlowableExtension extends InternalFlowableExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(PluggableFlowableExtension.class);
    private static final String PROCESS_ENGINE = "cachedProcessEngine";

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        try {
            super.afterEach(context);
        } finally {
            if (AnnotationSupport.isAnnotated(context.getRequiredTestClass(), EnableVerboseExecutionTreeLogging.class)) {
                swapCommandInvoker(getProcessEngine(context), false);
            }
        }
    }

    @Override
    protected ProcessEngine getProcessEngine(ExtensionContext context) {
        ProcessEngine processEngine = getStore(context).getOrComputeIfAbsent(PROCESS_ENGINE, key -> initializeProcessEngine(), ProcessEngine.class);

        // Enable verbose execution tree debugging if needed
        Class<?> testClass = context.getRequiredTestClass();
        if (AnnotationSupport.isAnnotated(testClass, EnableVerboseExecutionTreeLogging.class)) {
            swapCommandInvoker(processEngine, true);
        }
        return processEngine;
    }

    protected ProcessEngine initializeProcessEngine() {
        logger.info("No cached process engine found for test. Retrieving the default engine.");
        ProcessEngines.destroy(); // Just to be sure we're not getting any previously cached version

        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        if (processEngine == null) {
            throw new FlowableException("no default process engine available");
        }
        return processEngine;
    }

    protected void swapCommandInvoker(ProcessEngine processEngine, boolean debug) {
        CommandExecutor commandExecutor = processEngine.getProcessEngineConfiguration().getCommandExecutor();
        if (commandExecutor instanceof CommandExecutorImpl) {
            CommandExecutorImpl commandExecutorImpl = (CommandExecutorImpl) commandExecutor;

            CommandInterceptor previousCommandInterceptor = null;
            CommandInterceptor commandInterceptor = commandExecutorImpl.getFirst();

            while (commandInterceptor != null) {

                boolean matches = debug ? (commandInterceptor instanceof CommandInvoker) : (commandInterceptor instanceof LoggingExecutionTreeCommandInvoker);
                if (matches) {

                    CommandInterceptor commandInvoker = debug ? new LoggingExecutionTreeCommandInvoker() : new CommandInvoker();
                    if (previousCommandInterceptor != null) {
                        previousCommandInterceptor.setNext(commandInvoker);
                    } else {
                        commandExecutorImpl.setFirst(previousCommandInterceptor);
                    }
                    break;

                } else {
                    previousCommandInterceptor = commandInterceptor;
                    commandInterceptor = commandInterceptor.getNext();
                }
            }

        } else {
            logger.warn("Not using {}, ignoring the {} annotation", CommandExecutorImpl.class, EnableVerboseExecutionTreeLogging.class);
        }
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
