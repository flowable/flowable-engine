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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Joram Barrez
 */
public class MultiTenantSendEventTaskTest extends FlowableEventRegistryCmmnTestCase {

    private static final String TENANT_A = "tenantA";

    private static final String TENANT_B = "tenantB";

    protected TestOutboundEventChannelAdapter outboundEventChannelAdapter;

    private Set<String> cleanupDeploymentIds = new HashSet<>();

    @Before
    public void registerEventDefinition() {
        getEventRegistryEngineConfiguration().setFallbackToDefaultTenant(true);
        outboundEventChannelAdapter = setupTestChannel();

        getEventRepositoryService().createEventModelBuilder()
                .key("testEvent")
                .resourceName("testEvent.event")
                .payload("tenantACustomerId", EventPayloadTypes.STRING)
                .deploymentTenantId(TENANT_A)
                .deploy();

        getEventRepositoryService().createEventModelBuilder()
                .key("testEvent")
                .resourceName("testEvent.event")
                .payload("tenantBCustomerId", EventPayloadTypes.STRING)
                .deploymentTenantId(TENANT_B)
                .deploy();
    }

    protected TestOutboundEventChannelAdapter setupTestChannel() {
        TestOutboundEventChannelAdapter outboundEventChannelAdapter = new TestOutboundEventChannelAdapter();
        getEventRegistryEngineConfiguration().getExpressionManager().getBeans()
                .put("outboundEventChannelAdapter", outboundEventChannelAdapter);

        getEventRepositoryService().createOutboundChannelModelBuilder()
                .key("out-channel")
                .resourceName("out.channel")
                .channelAdapter("${outboundEventChannelAdapter}")
                .jsonSerializer()
                .deploy();

        return outboundEventChannelAdapter;
    }

    @After
    public void cleanup() {
        getEventRepositoryService().createDeploymentQuery().list()
                .forEach(eventDeployment -> getEventRepositoryService().deleteDeployment(eventDeployment.getId()));

        for (String cleanupDeploymentId : cleanupDeploymentIds) {
            CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, cleanupDeploymentId);
        }
        cleanupDeploymentIds.clear();

        getEventRegistryEngineConfiguration().setFallbackToDefaultTenant(false);
    }

    @Test
    public void testSimpleSendEvent() throws Exception {
        deployCaseModel("tenantA.cmmn", TENANT_A);
        deployCaseModel("tenantB.cmmn", TENANT_B);

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSendEvent")
                .variable("myVariable", "Hello tenantA")
                .tenantId(TENANT_A)
                .start();
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = cmmnEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(1);
        assertThat(jsonNode.get("tenantACustomerId").asText()).isEqualTo("Hello tenantA");

        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSendEvent")
                .variable("myVariable", "Hello tenantB")
                .tenantId(TENANT_B)
                .start();
        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(2);

        jsonNode = cmmnEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(1));
        assertThat(jsonNode).hasSize(1);
        assertThat(jsonNode.get("tenantBCustomerId").asText())
                .isEqualTo("Hello tenantB"); // Note: a different json (different event definition for different tenant)
    }

    public static class TestOutboundEventChannelAdapter implements OutboundEventChannelAdapter<String> {

        public List<String> receivedEvents = new ArrayList<>();

        @Override
        public void sendEvent(String rawEvent) {
            receivedEvents.add(rawEvent);
        }

    }

    private void deployCaseModel(String modelResource, String tenantId) {
        String resource = getClass().getPackage().toString().replace("package ", "").replace(".", "/");
        resource += "/MultiTenantSendEventTaskTest." + modelResource;
        CmmnDeploymentBuilder cmmnDeploymentBuilder = cmmnRepositoryService.createDeployment().addClasspathResource(resource);
        if (tenantId != null) {
            cmmnDeploymentBuilder.tenantId(tenantId);
        }

        String deploymentId = cmmnDeploymentBuilder.deploy().getId();
        cleanupDeploymentIds.add(deploymentId);

        assertThat(cmmnRepositoryService.createCaseDefinitionQuery().deploymentId(deploymentId).singleResult()).isNotNull();
    }

}
