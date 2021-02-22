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
package org.flowable.eventregistry.test.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.eventregistry.test.AbstractFlowableEventTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class DeploymentQueryTest extends AbstractFlowableEventTest {

    private String deploymentId1;
    private String deploymentId2;
    private String deploymentId3;

    private String eventModel = "{\n" +
            "    \"key\": \"$changeme$\",\n" +
            "    \"name\": \"My first event\"\n" +
            "}\n" +
            "";

    private String channelModel = "{\n" +
            "    \"key\": \"$changeme$\",\n" +
            "    \"name\": \"My first event\",\n" +
            "    \"channelType\": \"inbound\",\n" +
            "    \"type\": \"jms\",\n" +
            "    \"destination\": \"$changemedestination$\",\n" +
            "    \"deserializerType\": \"json\",\n" +
            "    \"channelEventKeyDetection\": {\n" +
            "       \"fixedValue\": \"$changemefixedvalue$\"\n" +
            "    }\n" +
            "}\n" +
            "";

    @BeforeEach
    public void deploy() {
        deploymentId1 = repositoryService.createDeployment()
                .name("test1.event")
                .category("testCategoryA")
                .addString("event1.event", eventModel.replace("$changeme$", "event1").replace("$changemechannel$", "channel1"))
                .tenantId("tenantA")
                .deploy().getId();

        deploymentId2 = repositoryService.createDeployment()
                .name("test2.event")
                .category("testCategoryB")
                .addString("event2.event", eventModel.replace("$changeme$", "event2").replace("$changemechannel$", "channel2"))
                .tenantId("tenantA")
                .deploy().getId();

        deploymentId3 = repositoryService.createDeployment()
                .name("test3.event")
                .category("testCategoryC")
                .addString("event3.event", eventModel.replace("$changeme$", "event3").replace("$changemechannel$", "channel3"))
                .addString("channel1.channel",
                        channelModel.replace("$changeme$", "channel1").replace("$changemedestination$", "testQueue").replace("$changemefixedvalue$", "myEvent"))
                .tenantId("tenantB")
                .deploy().getId();
    }

    @AfterEach
    public void cleanup() {
        repositoryService.deleteDeployment(deploymentId1);
        repositoryService.deleteDeployment(deploymentId2);
        repositoryService.deleteDeployment(deploymentId3);
    }

    @Test
    public void testQueryById() {
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult()).isNotNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).count()).isEqualTo(1);

        assertThat(repositoryService.createDeploymentQuery().deploymentId("invalid").singleResult()).isNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentId("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentId("invalid").count()).isZero();
    }

    @Test
    public void testQueryByName() {
        assertThat(repositoryService.createDeploymentQuery().deploymentName("test2.event").singleResult()).isNotNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentName("test2.event").list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentName("test2.event").count()).isEqualTo(1);

        assertThat(repositoryService.createDeploymentQuery().deploymentName("invalid").singleResult()).isNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentName("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentName("invalid").count()).isZero();
    }

    @Test
    public void testQueryByNameLike() {
        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike("test%").list()).hasSize(3);
        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike("test%").count()).isEqualTo(3);

        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike("inva%").singleResult()).isNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike("inva").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentNameLike("inva").count()).isZero();
    }

    @Test
    public void testQueryByCategory() {
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").singleResult()).isNotNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").count()).isEqualTo(1);

        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("inva%").singleResult()).isNull();
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("inva%").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentCategory("inva%").count()).isZero();
    }

    @Test
    public void testQueryByCategoryNotEquals() {
        assertThat(repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategoryC").list()).hasSize(2);
        assertThat(repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategoryC").count()).isEqualTo(2);

        assertThat(repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("invalid").list()).hasSize(3);
        assertThat(repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("invalid").count()).isEqualTo(3);
    }

    @Test
    public void testQueryByTenantId() {
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId("tenantA").list()).hasSize(2);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId("tenantA").count()).isEqualTo(2);

        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantId("invalid").count()).isZero();
    }

    @Test
    public void testQueryByTenantIdLike() {
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantIdLike("tenant%").list()).hasSize(3);
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantIdLike("tenant%").count()).isEqualTo(3);

        assertThat(repositoryService.createDeploymentQuery().deploymentTenantIdLike("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().deploymentTenantIdLike("invalid").count()).isZero();
    }

    @Test
    public void testQueryByEventDefinitionKey() {
        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKey("event2").list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKey("event2").count()).isEqualTo(1);

        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKey("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKey("invalid").count()).isZero();
    }

    @Test
    public void testQueryByEventDefinitionKeyLike() {
        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKeyLike("event%").list()).hasSize(3);
        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKeyLike("event%").count()).isEqualTo(3);
        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKeyLike("event%").listPage(0, 1)).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKeyLike("event%").listPage(0, 2)).hasSize(2);
        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKeyLike("event%").listPage(1, 2)).hasSize(2);

        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKeyLike("inva%").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().eventDefinitionKeyLike("inva%").count()).isZero();
    }

    @Test
    public void testQueryByChannelDefinitionKey() {
        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKey("channel1").list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKey("channel1").count()).isEqualTo(1);

        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKey("invalid").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKey("invalid").count()).isZero();
    }

    @Test
    public void testQueryByChannelDefinitionKeyLike() {
        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKeyLike("channel%").list()).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKeyLike("channel%").count()).isEqualTo(1);
        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKeyLike("channel%").listPage(0, 1)).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKeyLike("channel%").listPage(0, 2)).hasSize(1);
        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKeyLike("channel%").listPage(1, 2)).isEmpty();

        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKeyLike("inva%").list()).isEmpty();
        assertThat(repositoryService.createDeploymentQuery().channelDefinitionKeyLike("inva%").count()).isZero();
    }

}
