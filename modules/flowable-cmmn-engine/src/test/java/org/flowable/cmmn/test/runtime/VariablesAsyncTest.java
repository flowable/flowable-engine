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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.impl.job.SetAsyncVariablesJobHandler;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.job.api.Job;
import org.junit.Test;

public class VariablesAsyncTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSetVariableAsync() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();

        Map<String, Object> vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(2);
        assertThat(vars.get("stringVar")).isEqualTo("Hello World");
        assertThat(vars.get("intVar")).isEqualTo(42);

        cmmnRuntimeService.setVariableAsync(caseInstance.getId(), "extraVar", "test");
        vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(2);
        
        Job asyncVarJob = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).handlerType(SetAsyncVariablesJobHandler.TYPE).singleResult();
        assertThat(asyncVarJob).isNotNull();
        
        cmmnManagementService.executeJob(asyncVarJob.getId());
        
        vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(3);
        assertThat(vars.get("stringVar")).isEqualTo("Hello World");
        assertThat(vars.get("intVar")).isEqualTo(42);
        assertThat(vars.get("extraVar")).isEqualTo("test");
    }
    
    @Test
    @CmmnDeployment
    public void testSetVariablesAsync() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();

        Map<String, Object> vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(2);
        assertThat(vars.get("stringVar")).isEqualTo("Hello World");
        assertThat(vars.get("intVar")).isEqualTo(42);

        Map<String, Object> extraVariables = new HashMap<>();
        extraVariables.put("extraVar", "test");
        extraVariables.put("extraIntVar", 77);
        cmmnRuntimeService.setVariablesAsync(caseInstance.getId(), extraVariables);
        vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(2);
        
        Job asyncVarJob = cmmnManagementService.createJobQuery().caseInstanceId(caseInstance.getId()).handlerType(SetAsyncVariablesJobHandler.TYPE).singleResult();
        assertThat(asyncVarJob).isNotNull();
        
        cmmnManagementService.executeJob(asyncVarJob.getId());
        
        vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(4);
        assertThat(vars.get("stringVar")).isEqualTo("Hello World");
        assertThat(vars.get("intVar")).isEqualTo(42);
        assertThat(vars.get("extraVar")).isEqualTo("test");
        assertThat(vars.get("extraIntVar")).isEqualTo(77);
    }
    
    @Test
    @CmmnDeployment
    public void testSetLocalVariableAsync() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();

        Map<String, Object> vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(2);
        assertThat(vars.get("stringVar")).isEqualTo("Hello World");
        assertThat(vars.get("intVar")).isEqualTo(42);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        cmmnRuntimeService.setLocalVariableAsync(planItemInstance.getId(), "extraVar", "test");
        vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(2);
        
        Job asyncVarJob = cmmnManagementService.createJobQuery().planItemInstanceId(planItemInstance.getId()).handlerType(SetAsyncVariablesJobHandler.TYPE).singleResult();
        assertThat(asyncVarJob).isNotNull();
        
        cmmnManagementService.executeJob(asyncVarJob.getId());
        
        vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(2);
        assertThat(vars.get("stringVar")).isEqualTo("Hello World");
        assertThat(vars.get("intVar")).isEqualTo(42);
        
        vars = cmmnRuntimeService.getLocalVariables(planItemInstance.getId());
        assertThat(vars).hasSize(1);
        assertThat(vars.get("extraVar")).isEqualTo("test");
    }
    
    @Test
    @CmmnDeployment
    public void testSetLocalVariablesAsync() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Hello World");
        variables.put("intVar", 42);
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").variables(variables).start();

        Map<String, Object> vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(2);
        assertThat(vars.get("stringVar")).isEqualTo("Hello World");
        assertThat(vars.get("intVar")).isEqualTo(42);

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().caseInstanceId(caseInstance.getId()).singleResult();
        Map<String, Object> extraVariables = new HashMap<>();
        extraVariables.put("extraVar", "test");
        extraVariables.put("extraIntVar", 77);
        cmmnRuntimeService.setLocalVariablesAsync(planItemInstance.getId(), extraVariables);
        vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(2);
        
        Job asyncVarJob = cmmnManagementService.createJobQuery().planItemInstanceId(planItemInstance.getId()).handlerType(SetAsyncVariablesJobHandler.TYPE).singleResult();
        assertThat(asyncVarJob).isNotNull();
        
        cmmnManagementService.executeJob(asyncVarJob.getId());
        
        vars = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(vars).hasSize(2);
        assertThat(vars.get("stringVar")).isEqualTo("Hello World");
        assertThat(vars.get("intVar")).isEqualTo(42);
        
        vars = cmmnRuntimeService.getLocalVariables(planItemInstance.getId());
        assertThat(vars).hasSize(2);
        assertThat(vars.get("extraVar")).isEqualTo("test");
        assertThat(vars.get("extraIntVar")).isEqualTo(77);
    }

}
