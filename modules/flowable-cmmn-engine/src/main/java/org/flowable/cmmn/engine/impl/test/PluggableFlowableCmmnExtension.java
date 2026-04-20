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
package org.flowable.cmmn.engine.impl.test;

import static org.flowable.cmmn.engine.test.FlowableCmmnExtension.DEFAULT_CONFIGURATION_RESOURCE;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.cmmn.engine.test.CmmnConfigurationResource;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * @author Filip Hrisafov
 */
public class PluggableFlowableCmmnExtension extends InternalFlowableCmmnExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(PluggableFlowableCmmnExtension.class);

    @Override
    protected CmmnEngine getCmmnEngine(ExtensionContext context) {
        String configurationResource = getConfigurationResource(context);
        return getStore(context).computeIfAbsent(configurationResource, this::initializeCmmnEngine, CmmnEngine.class);
    }

    protected CmmnEngine initializeCmmnEngine(String configurationResource) {
        logger.info("No cached cmmn engine found for test. Retrieving engine from {}.", configurationResource);
        CmmnEngineConfiguration engineConfiguration = CmmnEngineConfiguration.createCmmnEngineConfigurationFromResource(configurationResource);
        if (CmmnEngines.isInitialized()) {
            CmmnEngine previousEngine = CmmnEngines.getCmmnEngine(engineConfiguration.getEngineName());
            if (previousEngine != null) {
                CmmnEngines.unregister(previousEngine); // Just to be sure we're not getting any previously cached version
                previousEngine.close();
            }
        }
        CmmnEngine cmmnEngine = engineConfiguration.buildEngine();
        CmmnEngines.setInitialized(true);
        return cmmnEngine;
    }

    protected String getConfigurationResource(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), CmmnConfigurationResource.class)
                .map(CmmnConfigurationResource::value)
                .orElse(DEFAULT_CONFIGURATION_RESOURCE);
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }

}
