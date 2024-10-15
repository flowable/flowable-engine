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
package org.flowable.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.jobexecutor.SetAsyncVariablesJobHandler;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

public class VariableAsyncTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testSetVariableAsync() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "John Doe");
        vars.put("amount", 99);
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(2);
        assertThat(vars.get("name")).isEqualTo("John Doe");
        assertThat(vars.get("amount")).isEqualTo(99);
        
        runtimeService.setVariableAsync(processInstanceId, "asyncVar", "test");
        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(2);
        
        Job asyncVarJob = managementService.createJobQuery().processInstanceId(processInstanceId).handlerType(SetAsyncVariablesJobHandler.TYPE).singleResult();
        assertThat(asyncVarJob).isNotNull();
        
        managementService.executeJob(asyncVarJob.getId());
        
        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(3);
        assertThat(vars.get("name")).isEqualTo("John Doe");
        assertThat(vars.get("amount")).isEqualTo(99);
        assertThat(vars.get("asyncVar")).isEqualTo("test");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testSetVariablesAsync() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "John Doe");
        vars.put("amount", 99);
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(2);
        assertThat(vars.get("name")).isEqualTo("John Doe");
        assertThat(vars.get("amount")).isEqualTo(99);
        
        Map<String, Object> asyncVars = new HashMap<>();
        asyncVars.put("asyncVar", "test");
        asyncVars.put("anotherVar", 33);
        runtimeService.setVariablesAsync(processInstanceId, asyncVars);
        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(2);
        
        Job asyncVarJob = managementService.createJobQuery().processInstanceId(processInstanceId).handlerType(SetAsyncVariablesJobHandler.TYPE).singleResult();
        assertThat(asyncVarJob).isNotNull();
        
        managementService.executeJob(asyncVarJob.getId());
        
        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(4);
        assertThat(vars.get("name")).isEqualTo("John Doe");
        assertThat(vars.get("amount")).isEqualTo(99);
        assertThat(vars.get("asyncVar")).isEqualTo("test");
        assertThat(vars.get("anotherVar")).isEqualTo(33);
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testSetVariableLocalAsync() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "John Doe");
        vars.put("amount", 99);
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(2);
        assertThat(vars.get("name")).isEqualTo("John Doe");
        assertThat(vars.get("amount")).isEqualTo(99);
        
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).onlyChildExecutions().singleResult();
        
        runtimeService.setVariableLocalAsync(execution.getId(), "asyncVar", "test");
        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(2);
        vars = runtimeService.getVariablesLocal(execution.getId());
        assertThat(vars).hasSize(0);
        
        Job asyncVarJob = managementService.createJobQuery().processInstanceId(processInstanceId).handlerType(SetAsyncVariablesJobHandler.TYPE).singleResult();
        assertThat(asyncVarJob).isNotNull();
        
        managementService.executeJob(asyncVarJob.getId());
        
        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(2);
        
        vars = runtimeService.getVariablesLocal(execution.getId());
        assertThat(vars).hasSize(1);
        assertThat(vars.get("asyncVar")).isEqualTo("test");
    }
    
    @Test
    @Deployment(resources = "org/flowable/engine/test/api/variables/VariablesTest.bpmn20.xml")
    public void testSetVariablesLocalAsync() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "John Doe");
        vars.put("amount", 99);
        String processInstanceId = runtimeService.startProcessInstanceByKey("variablesTest", vars).getId();

        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(2);
        assertThat(vars.get("name")).isEqualTo("John Doe");
        assertThat(vars.get("amount")).isEqualTo(99);
        
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceId).onlyChildExecutions().singleResult();
        
        Map<String, Object> asyncVars = new HashMap<>();
        asyncVars.put("asyncVar", "test");
        asyncVars.put("anotherVar", 33);
        runtimeService.setVariablesLocalAsync(execution.getId(), asyncVars);
        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(2);
        
        Job asyncVarJob = managementService.createJobQuery().processInstanceId(processInstanceId).handlerType(SetAsyncVariablesJobHandler.TYPE).singleResult();
        assertThat(asyncVarJob).isNotNull();
        
        managementService.executeJob(asyncVarJob.getId());
        
        vars = runtimeService.getVariables(processInstanceId);
        assertThat(vars).hasSize(2);
        assertThat(vars.get("name")).isEqualTo("John Doe");
        assertThat(vars.get("amount")).isEqualTo(99);
        
        vars = runtimeService.getVariablesLocal(execution.getId());
        assertThat(vars).hasSize(2);
        assertThat(vars.get("asyncVar")).isEqualTo("test");
        assertThat(vars.get("anotherVar")).isEqualTo(33);
    }
}
