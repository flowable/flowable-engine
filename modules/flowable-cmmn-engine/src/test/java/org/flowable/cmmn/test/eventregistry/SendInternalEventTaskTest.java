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
package org.flowable.cmmn.test.eventregistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.eventregistry.api.EventDeployment;
import org.junit.After;
import org.junit.Test;

/**
 * @author Filip Hrisafov
 */
public class SendInternalEventTaskTest extends FlowableEventRegistryCmmnTestCase {

    @After
    public void tearDown() {
        List<EventDeployment> eventDeployments = getEventRepositoryService().createDeploymentQuery().list();

        for (EventDeployment eventDeployment : eventDeployments) {
            getEventRepositoryService().deleteDeployment(eventDeployment.getId());
        }

    }

    @Test
    @CmmnDeployment(resources = {
            "org/flowable/cmmn/test/eventregistry/SendInternalEventTaskTest.testSendEvent.cmmn",
            "org/flowable/cmmn/test/eventregistry/SendInternalEventTaskTest.testCaseStartWithPayload.cmmn.xml",
            "org/flowable/cmmn/test/eventregistry/SendInternalEventTaskTest.testCaseStartOtherWithPayload.cmmn.xml",
            "org/flowable/cmmn/test/eventregistry/SendInternalEventTaskTest.simple.event"
    })
    public void testSendEvent() {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSendEvent")
                .variable("customerId", "kermit")
                .variable("customerName", "Kermit the Frog")
                .variable("eventKey", "simpleTest")
                .variable("action", "start")
                .start();

        CaseInstance caseStartEvent = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("caseStartEventWithPayload")
                .includeCaseVariables()
                .singleResult();

        assertThat(caseStartEvent).isNotNull();
        assertThat(caseStartEvent.getCaseVariables())
                .containsOnly(
                        entry("customerId", "kermit"),
                        entry("customerName", "Kermit the Frog")
                );

        CaseInstance caseStartOtherEvent = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("caseStartOtherEventWithPayload")
                .includeCaseVariables()
                .singleResult();
        assertThat(caseStartOtherEvent).isNull();

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSendEvent")
                .variable("customerId", "kermit")
                .variable("customerName", "Kermit")
                .variable("eventKey", "simpleTest")
                .variable("action", "startOther")
                .start();

        caseStartEvent = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("caseStartEventWithPayload")
                .includeCaseVariables()
                .singleResult();

        assertThat(caseStartEvent).isNotNull();

        caseStartOtherEvent = cmmnRuntimeService.createCaseInstanceQuery()
                .caseDefinitionKey("caseStartOtherEventWithPayload")
                .includeCaseVariables()
                .singleResult();
        assertThat(caseStartOtherEvent).isNotNull();
        assertThat(caseStartOtherEvent.getCaseVariables())
                .containsOnly(
                        entry("customer", "kermit"),
                        entry("name", "Kermit")
                );
    }
}
