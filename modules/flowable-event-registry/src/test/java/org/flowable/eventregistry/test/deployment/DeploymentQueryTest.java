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

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
            "    \"name\": \"My first event\",\n" + 
            "    \"inboundChannelKeys\": [\n" + 
            "       \"$changemechannel\"\n" + 
            "    ]\n" + 
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
                .addString("channel1.channel", channelModel.replace("$changeme$", "channel1").replace("$changemedestination$", "testQueue").replace("$changemefixedvalue$", "myEvent"))
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
        assertNotNull(repositoryService.createDeploymentQuery().deploymentId(deploymentId1).singleResult());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentId(deploymentId1).list().size());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentId(deploymentId1).count());
        
        assertNull(repositoryService.createDeploymentQuery().deploymentId("invalid").singleResult());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentId("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentId("invalid").count());
    }
    
    @Test
    public void testQueryByName() {
        assertNotNull(repositoryService.createDeploymentQuery().deploymentName("test2.event").singleResult());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentName("test2.event").list().size());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentName("test2.event").count());
        
        assertNull(repositoryService.createDeploymentQuery().deploymentName("invalid").singleResult());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentName("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentName("invalid").count());
    }
    
    @Test
    public void testQueryByNameLike() {
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentNameLike("test%").list().size());
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentNameLike("test%").count());
        
        assertNull(repositoryService.createDeploymentQuery().deploymentNameLike("inva%").singleResult());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentNameLike("inva").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentNameLike("inva").count());
    }
    
    @Test
    public void testQueryByCategory() {
        assertNotNull(repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").singleResult());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").list().size());
        assertEquals(1, repositoryService.createDeploymentQuery().deploymentCategory("testCategoryC").count());
        
        assertNull(repositoryService.createDeploymentQuery().deploymentCategory("inva%").singleResult());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentCategory("inva%").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentCategory("inva%").count());
    }
    
    @Test
    public void testQueryByCategoryNotEquals() {
        assertEquals(2, repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategoryC").list().size());
        assertEquals(2, repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("testCategoryC").count());
        
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("invalid").list().size());
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentCategoryNotEquals("invalid").count());
    }
    
    @Test
    public void testQueryByTenantId() {
        assertEquals(2, repositoryService.createDeploymentQuery().deploymentTenantId("tenantA").list().size());
        assertEquals(2, repositoryService.createDeploymentQuery().deploymentTenantId("tenantA").count());
        
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentTenantId("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentTenantId("invalid").count());
    }
    
    @Test
    public void testQueryByTenantIdLike() {
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentTenantIdLike("tenant%").list().size());
        assertEquals(3, repositoryService.createDeploymentQuery().deploymentTenantIdLike("tenant%").count());
        
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentTenantIdLike("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().deploymentTenantIdLike("invalid").count());
    }
    
    @Test
    public void testQueryByEventDefinitionKey() {
        assertEquals(1, repositoryService.createDeploymentQuery().eventDefinitionKey("event2").list().size());
        assertEquals(1, repositoryService.createDeploymentQuery().eventDefinitionKey("event2").count());
        
        assertEquals(0, repositoryService.createDeploymentQuery().eventDefinitionKey("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().eventDefinitionKey("invalid").count());
    }
    
    @Test
    public void testQueryByEventDefinitionKeyLike() {
        assertEquals(3, repositoryService.createDeploymentQuery().eventDefinitionKeyLike("event%").list().size());
        assertEquals(3, repositoryService.createDeploymentQuery().eventDefinitionKeyLike("event%").count());
        assertEquals(1, repositoryService.createDeploymentQuery().eventDefinitionKeyLike("event%").listPage(0, 1).size());
        assertEquals(2, repositoryService.createDeploymentQuery().eventDefinitionKeyLike("event%").listPage(0, 2).size());
        assertEquals(2, repositoryService.createDeploymentQuery().eventDefinitionKeyLike("event%").listPage(1, 2).size());
        
        assertEquals(0, repositoryService.createDeploymentQuery().eventDefinitionKeyLike("inva%").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().eventDefinitionKeyLike("inva%").count());
    }
    
    @Test
    public void testQueryByChannelDefinitionKey() {
        assertEquals(1, repositoryService.createDeploymentQuery().channelDefinitionKey("channel1").list().size());
        assertEquals(1, repositoryService.createDeploymentQuery().channelDefinitionKey("channel1").count());
        
        assertEquals(0, repositoryService.createDeploymentQuery().channelDefinitionKey("invalid").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().channelDefinitionKey("invalid").count());
    }
    
    @Test
    public void testQueryByChannelDefinitionKeyLike() {
        assertEquals(1, repositoryService.createDeploymentQuery().channelDefinitionKeyLike("channel%").list().size());
        assertEquals(1, repositoryService.createDeploymentQuery().channelDefinitionKeyLike("channel%").count());
        assertEquals(1, repositoryService.createDeploymentQuery().channelDefinitionKeyLike("channel%").listPage(0, 1).size());
        assertEquals(1, repositoryService.createDeploymentQuery().channelDefinitionKeyLike("channel%").listPage(0, 2).size());
        assertEquals(0, repositoryService.createDeploymentQuery().channelDefinitionKeyLike("channel%").listPage(1, 2).size());
        
        assertEquals(0, repositoryService.createDeploymentQuery().channelDefinitionKeyLike("inva%").list().size());
        assertEquals(0, repositoryService.createDeploymentQuery().channelDefinitionKeyLike("inva%").count());
    }
    
}
