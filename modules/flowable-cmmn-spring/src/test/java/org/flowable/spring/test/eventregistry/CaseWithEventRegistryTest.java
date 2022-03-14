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
package org.flowable.spring.test.eventregistry;

import java.util.List;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@JmsEventTest
@TestPropertySource(properties = {
        "application.test.jms-queue=test-expression-customer"
})
public class CaseWithEventRegistryTest {
    
    @Autowired
    protected CmmnEngine cmmnEngine;
    
    @Autowired
    protected CmmnRuntimeService cmmnRuntimeService;

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/spring/test/startCaseWithEvent.cmmn",
            "org/flowable/cmmn/spring/test/one.event",
            "org/flowable/cmmn/spring/test/one.channel"})
    public void testStartCaseWithEvent() {
        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testCaseStartEvent").start();

        } finally {
            List<EventDeployment> eventDeployments = getEventRepositoryService().createDeploymentQuery().list();
            for (EventDeployment eventDeployment : eventDeployments) {
                getEventRepositoryService().deleteDeployment(eventDeployment.getId());
            }
        }
    }
    
    protected EventRepositoryService getEventRepositoryService() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) 
                cmmnEngine.getCmmnEngineConfiguration().getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRepositoryService();
    }
}
