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
package org.flowable.dmn.engine.test.history;

import org.flowable.dmn.api.DmnHistoricDecisionExecution;
import org.flowable.dmn.engine.impl.test.PluggableFlowableDmnTestCase;
import org.flowable.dmn.engine.test.DmnDeployment;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Tijs Rademakers
 */
public class HistoryTest extends PluggableFlowableDmnTestCase {

    @DmnDeployment
    public void testFirstHitPolicy() throws Exception {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 11)
                .executeWithSingleResult();
        
        DmnHistoricDecisionExecution decisionExecution = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").singleResult();
        assertNotNull(decisionExecution.getDecisionDefinitionId());
        assertNotNull(decisionExecution.getDeploymentId());
        assertFalse(decisionExecution.isFailed());
        assertNotNull(decisionExecution.getStartTime());
        assertNotNull(decisionExecution.getEndTime());
        assertNotNull(decisionExecution.getExecutionJson());
        
        JsonNode executionNode = dmnEngineConfiguration.getObjectMapper().readTree(decisionExecution.getExecutionJson());
        assertEquals("decision1", executionNode.get("decisionKey").asText());
        assertEquals("Full Decision", executionNode.get("decisionName").asText());
        assertEquals("FIRST", executionNode.get("hitPolicy").asText());
        
        JsonNode inputVariables = executionNode.get("inputVariables");
        assertTrue(inputVariables.isObject());
        assertTrue(inputVariables.has("inputVariable1"));
        assertEquals(11, inputVariables.get("inputVariable1").asLong());
        
        JsonNode inputVariableTypes = executionNode.get("inputVariableTypes");
        assertTrue(inputVariableTypes.isObject());
        assertTrue(inputVariableTypes.has("inputVariable1"));
        assertEquals("number", inputVariableTypes.get("inputVariable1").asText());
        
        JsonNode decisionResultArray = executionNode.get("decisionResult");
        assertTrue(decisionResultArray.isArray());
        assertEquals(1, decisionResultArray.size());
        JsonNode decisionResultNode = decisionResultArray.get(0);
        assertTrue(decisionResultNode.has("outputVariable1"));
        assertTrue(decisionResultNode.has("outputVariable2"));
        assertEquals("gt 10", decisionResultNode.get("outputVariable1").asText());
        assertEquals("result2", decisionResultNode.get("outputVariable2").asText());
        
        JsonNode decisionResultTypes = executionNode.get("decisionResultTypes");
        assertTrue(decisionResultTypes.isObject());
        assertTrue(decisionResultTypes.has("outputVariable1"));
        assertEquals("string", decisionResultTypes.get("outputVariable1").asText());
        assertTrue(decisionResultTypes.has("outputVariable2"));
        assertEquals("string", decisionResultTypes.get("outputVariable2").asText());
        
        JsonNode ruleExecutions = executionNode.get("ruleExecutions");
        assertTrue(ruleExecutions.isObject());
        assertTrue(ruleExecutions.has("1"));
        assertFalse(ruleExecutions.get("1").get("valid").asBoolean());
        assertTrue(ruleExecutions.has("2"));
        assertTrue(ruleExecutions.get("2").get("valid").asBoolean());
    }
    
    @DmnDeployment
    public void testOutputOrderHitPolicy() throws Exception {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .variable("inputVariable1", 5)
                .execute();
        
        DmnHistoricDecisionExecution decisionExecution = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").singleResult();
        assertNotNull(decisionExecution.getDecisionDefinitionId());
        assertNotNull(decisionExecution.getDeploymentId());
        assertFalse(decisionExecution.isFailed());
        assertNotNull(decisionExecution.getStartTime());
        assertNotNull(decisionExecution.getEndTime());
        assertNotNull(decisionExecution.getExecutionJson());
        
        JsonNode executionNode = dmnEngineConfiguration.getObjectMapper().readTree(decisionExecution.getExecutionJson());
        assertEquals("decision1", executionNode.get("decisionKey").asText());
        assertEquals("Full Decision", executionNode.get("decisionName").asText());
        assertEquals("OUTPUT ORDER", executionNode.get("hitPolicy").asText());
        
        JsonNode inputVariables = executionNode.get("inputVariables");
        assertTrue(inputVariables.isObject());
        assertTrue(inputVariables.has("inputVariable1"));
        assertEquals(5, inputVariables.get("inputVariable1").asLong());
        
        JsonNode inputVariableTypes = executionNode.get("inputVariableTypes");
        assertTrue(inputVariableTypes.isObject());
        assertTrue(inputVariableTypes.has("inputVariable1"));
        assertEquals("number", inputVariableTypes.get("inputVariable1").asText());
        
        JsonNode decisionResultArray = executionNode.get("decisionResult");
        assertTrue(decisionResultArray.isArray());
        assertEquals(3, decisionResultArray.size());
        JsonNode decisionResultNode = decisionResultArray.get(0);
        assertTrue(decisionResultNode.has("outputVariable1"));
        assertEquals("OUTPUT2", decisionResultNode.get("outputVariable1").asText());
        
        decisionResultNode = decisionResultArray.get(1);
        assertTrue(decisionResultNode.has("outputVariable1"));
        assertEquals("OUTPUT3", decisionResultNode.get("outputVariable1").asText());
        
        decisionResultNode = decisionResultArray.get(2);
        assertTrue(decisionResultNode.has("outputVariable1"));
        assertEquals("OUTPUT1", decisionResultNode.get("outputVariable1").asText());
        
        
        JsonNode decisionResultTypes = executionNode.get("decisionResultTypes");
        assertTrue(decisionResultTypes.isObject());
        assertTrue(decisionResultTypes.has("outputVariable1"));
        assertEquals("string", decisionResultTypes.get("outputVariable1").asText());
        
        JsonNode ruleExecutions = executionNode.get("ruleExecutions");
        assertTrue(ruleExecutions.isObject());
        assertTrue(ruleExecutions.has("1"));
        assertTrue(ruleExecutions.get("1").get("valid").asBoolean());
        assertTrue(ruleExecutions.has("2"));
        assertTrue(ruleExecutions.get("2").get("valid").asBoolean());
        assertTrue(ruleExecutions.has("3"));
        assertTrue(ruleExecutions.get("3").get("valid").asBoolean());
    }
    
    @DmnDeployment
    public void testPriorityHitPolicy() throws Exception {
        ruleService.createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .instanceId("myInstanceId")
                .executionId("myExecutionId")
                .activityId("myActivityId")
                .variable("inputVariable1", 5)
                .executeWithSingleResult();
        
        assertEquals(1, historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").list().size());
        assertEquals(1, historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").instanceId("myInstanceId").list().size());
        assertEquals(0, historyService.createHistoricDecisionExecutionQuery().executionId("myInstanceId2").list().size());
        assertEquals(1, historyService.createHistoricDecisionExecutionQuery().executionId("myExecutionId").list().size());
        assertEquals(0, historyService.createHistoricDecisionExecutionQuery().executionId("myExecutionId2").list().size());
        assertEquals(1, historyService.createHistoricDecisionExecutionQuery().activityId("myActivityId").list().size());
        assertEquals(0, historyService.createHistoricDecisionExecutionQuery().activityId("myActivityId2").list().size());
        
        DmnHistoricDecisionExecution decisionExecution = historyService.createHistoricDecisionExecutionQuery().decisionKey("decision1").singleResult();
        assertNotNull(decisionExecution.getDecisionDefinitionId());
        assertNotNull(decisionExecution.getDeploymentId());
        assertEquals("myInstanceId", decisionExecution.getInstanceId());
        assertEquals("myExecutionId", decisionExecution.getExecutionId());
        assertEquals("myActivityId", decisionExecution.getActivityId());
        assertFalse(decisionExecution.isFailed());
        assertNotNull(decisionExecution.getStartTime());
        assertNotNull(decisionExecution.getEndTime());
        assertNotNull(decisionExecution.getExecutionJson());
        
        JsonNode executionNode = dmnEngineConfiguration.getObjectMapper().readTree(decisionExecution.getExecutionJson());
        assertEquals("decision1", executionNode.get("decisionKey").asText());
        assertEquals("Full Decision", executionNode.get("decisionName").asText());
        assertEquals("PRIORITY", executionNode.get("hitPolicy").asText());
        
        JsonNode inputVariables = executionNode.get("inputVariables");
        assertTrue(inputVariables.isObject());
        assertTrue(inputVariables.has("inputVariable1"));
        assertEquals(5, inputVariables.get("inputVariable1").asLong());
        
        JsonNode inputVariableTypes = executionNode.get("inputVariableTypes");
        assertTrue(inputVariableTypes.isObject());
        assertTrue(inputVariableTypes.has("inputVariable1"));
        assertEquals("number", inputVariableTypes.get("inputVariable1").asText());
        
        JsonNode decisionResultArray = executionNode.get("decisionResult");
        assertTrue(decisionResultArray.isArray());
        assertEquals(1, decisionResultArray.size());
        JsonNode decisionResultNode = decisionResultArray.get(0);
        assertTrue(decisionResultNode.has("outputVariable1"));
        assertEquals("OUTPUT2", decisionResultNode.get("outputVariable1").asText());
        
        JsonNode decisionResultTypes = executionNode.get("decisionResultTypes");
        assertTrue(decisionResultTypes.isObject());
        assertTrue(decisionResultTypes.has("outputVariable1"));
        assertEquals("string", decisionResultTypes.get("outputVariable1").asText());
        
        JsonNode ruleExecutions = executionNode.get("ruleExecutions");
        assertTrue(ruleExecutions.isObject());
        assertTrue(ruleExecutions.has("1"));
        assertTrue(ruleExecutions.get("1").get("valid").asBoolean());
        assertTrue(ruleExecutions.has("2"));
        assertTrue(ruleExecutions.get("2").get("valid").asBoolean());
        assertTrue(ruleExecutions.has("3"));
        assertTrue(ruleExecutions.get("3").get("valid").asBoolean());
    }
}
