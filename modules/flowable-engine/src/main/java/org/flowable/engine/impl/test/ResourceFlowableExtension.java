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

import java.util.function.Consumer;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngines;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * An extension that uses the configured resource to create a {@link ProcessEngine}.
 * This extension needs to be registered via {@link org.junit.jupiter.api.extension.RegisterExtension RegisterExtension}. It additionally allows for
 * customizing the {@link ProcessEngineConfiguration}
 * A new {@link ProcessEngine} will be created for each test.
 *
 * @author Filip Hrisafov
 */
public class ResourceFlowableExtension extends InternalFlowableExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(ResourceFlowableExtension.class);
    protected final String configurationResource;
    protected final String processEngineName;
    protected final Consumer<ProcessEngineConfiguration> configurationConsumer;

    public ResourceFlowableExtension(String configurationResource, Consumer<ProcessEngineConfiguration> configurationConsumer) {
        this(configurationResource, null, configurationConsumer);
    }

    public ResourceFlowableExtension(String configurationResource, String processEngineName, Consumer<ProcessEngineConfiguration> configurationConsumer) {
        this.configurationResource = configurationResource;
        this.processEngineName = processEngineName;
        this.configurationConsumer = configurationConsumer;
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        super.afterEach(context);
        ProcessEngine processEngine = getProcessEngine(context);
        ProcessEngines.unregister(processEngine);
    }

    @Override
    protected ProcessEngine getProcessEngine(ExtensionContext context) {
        return getStore(context).getOrComputeIfAbsent(context.getUniqueId(), key -> initializeProcessEngine(), ProcessEngine.class);
    }

    protected ProcessEngine initializeProcessEngine() {
        ProcessEngineConfiguration config = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource(configurationResource);
        if (processEngineName != null) {
            logger.info("Initializing process engine with name '{}'", processEngineName);
            config.setEngineName(processEngineName);
        }
        configurationConsumer.accept(config);
        ProcessEngine processEngine = config.buildProcessEngine();
        ProcessEngines.setInitialized(true);
        return processEngine;
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }

    public ProcessEngine rebootEngine() {
        String engineName = processEngineName != null ? processEngineName : ProcessEngines.NAME_DEFAULT;
        ProcessEngine processEngine = ProcessEngines.getProcessEngine(engineName);
        ProcessEngines.unregister(processEngine);
        return initializeProcessEngine();
    }
}
