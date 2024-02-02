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
package org.flowable.idm.engine.test;

import java.util.function.Consumer;

import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.IdmEngines;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * An extension that uses the configured resource to create an {@link IdmEngine}.
 * This extension needs to be registered via {@link org.junit.jupiter.api.extension.RegisterExtension RegisterExtension}. It additionally allows for
 * customizing the {@link IdmEngineConfiguration}
 * A new {@link IdmEngine} will be created for each test.
 *
 * @author Filip Hrisafov
 */
public class ResourceFlowableIdmExtension extends InternalFlowableIdmExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(ResourceFlowableIdmExtension.class);
    protected final String configurationResource;
    protected final String idmEngineName;
    protected final Consumer<IdmEngineConfiguration> configurationConsumer;

    public ResourceFlowableIdmExtension(String configurationResource, Consumer<IdmEngineConfiguration> configurationConsumer) {
        this(configurationResource, null, configurationConsumer);
    }

    public ResourceFlowableIdmExtension(String configurationResource, String idmEngineName, Consumer<IdmEngineConfiguration> configurationConsumer) {
        this.configurationResource = configurationResource;
        this.idmEngineName = idmEngineName;
        this.configurationConsumer = configurationConsumer;
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        super.afterEach(context);
        IdmEngine processEngine = getIdmEngine(context);
        IdmEngines.unregister(processEngine);
    }

    @Override
    protected IdmEngine getIdmEngine(ExtensionContext context) {
        return getStore(context).getOrComputeIfAbsent(context.getUniqueId(), key -> initializeIdmEngine(), IdmEngine.class);
    }

    protected IdmEngine initializeIdmEngine() {
        IdmEngineConfiguration config = IdmEngineConfiguration.createIdmEngineConfigurationFromResource(configurationResource);
        if (idmEngineName != null) {
            logger.info("Initializing idm engine with name '{}'", idmEngineName);
            config.setEngineName(idmEngineName);
        }
        configurationConsumer.accept(config);
        IdmEngine idmEngine = config.buildIdmEngine();
        IdmEngines.setInitialized(true);
        return idmEngine;
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
