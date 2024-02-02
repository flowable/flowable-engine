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
package org.flowable.engine.test.eventregistry;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Filip Hrisafov
 */
public class FlowableEventRegistryBpmnTestCase extends PluggableFlowableTestCase {

    protected Map<Object, Object> initialBeans;

    @BeforeEach
    public void setUpBeans() {
        ExpressionManager eventRegistryExpressionManager = getEventRegistryEngineConfiguration().getExpressionManager();
        initialBeans = eventRegistryExpressionManager.getBeans();
        eventRegistryExpressionManager.setBeans(new HashMap<>());
    }

    @AfterEach
    public void resetBeans() {
        getEventRegistryEngineConfiguration().getExpressionManager().setBeans(initialBeans);
    }

    protected EventRepositoryService getEventRepositoryService() {
        return getEventRegistryEngineConfiguration().getEventRepositoryService();
    }

    protected EventRegistry getEventRegistry() {
        return getEventRegistryEngineConfiguration().getEventRegistry();
    }

    protected EventRegistryEngineConfiguration getEventRegistryEngineConfiguration() {
        return (EventRegistryEngineConfiguration) processEngineConfiguration.getEngineConfigurations()
            .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
    }

}
