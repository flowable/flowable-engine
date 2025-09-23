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
package org.flowable.dmn.engine.impl.test;

import static org.flowable.dmn.engine.test.FlowableDmnExtension.DEFAULT_CONFIGURATION_RESOURCE;

import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.DmnEngines;
import org.flowable.dmn.engine.test.DmnConfigurationResource;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * @author Filip Hrisafov
 */
public class PluggableFlowableDmnExtension extends InternalFlowableDmnExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(PluggableFlowableDmnExtension.class);

    @Override
    protected DmnEngine getDmnEngine(ExtensionContext context) {
        String configurationResource = getConfigurationResource(context);
        return getStore(context).getOrComputeIfAbsent(configurationResource, this::initializeDmnEngine, CloseableEngine.class).dmnEngine;
    }

    protected CloseableEngine initializeDmnEngine(String configurationResource) {
        logger.info("No cached dmn engine found for test. Retrieving engine from {}.", configurationResource);
        DmnEngineConfiguration dmnEngineConfiguration = DmnEngineConfiguration.createDmnEngineConfigurationFromResource(configurationResource);
        DmnEngine dmnEngine = dmnEngineConfiguration.buildDmnEngine();
        DmnEngines.setInitialized(true);
        return new CloseableEngine(dmnEngine);
    }

    protected String getConfigurationResource(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), DmnConfigurationResource.class)
                .map(DmnConfigurationResource::value)
                .orElse(DEFAULT_CONFIGURATION_RESOURCE);
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }

    protected static class CloseableEngine implements AutoCloseable {

        protected final DmnEngine dmnEngine;

        protected CloseableEngine(DmnEngine dmnEngine) {
            this.dmnEngine = dmnEngine;
        }

        @Override
        public void close() {
            if (dmnEngine != null) {
                dmnEngine.close();
            }
        }
    }

    protected record ConfigurationResource(String resource, String engineName) {

    }

}
