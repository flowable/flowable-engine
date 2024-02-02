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

package org.flowable.spring.test.el;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.api.runtime.PlanItemInstanceState;
import org.flowable.cmmn.engine.test.FlowableCmmnRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 */
public class SpringCustomBeanTest {

    @Rule
    public FlowableCmmnRule cmmnRule = new FlowableCmmnRule("org/flowable/spring/test/el/SpringBeanTest-context.xml");

    @Test
    public void testSimpleCaseBean() {
        cmmnRule.getCmmnRepositoryService().createDeployment().addClasspathResource("org/flowable/spring/test/el/springExpression.cmmn").deploy();
        
        try {
            CmmnRuntimeService cmmnRuntimeService = cmmnRule.getCmmnRuntimeService();
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                            .caseDefinitionKey("myCase")
                            .variable("var1", "John Doe")
                            .start();
            
            assertThat(caseInstance).isNotNull();
            
            PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                            .caseInstanceId(caseInstance.getId())
                            .planItemInstanceState(PlanItemInstanceState.ACTIVE)
                            .singleResult();
            assertThat(planItemInstance).isNotNull();
            
            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "customResponse")).isEqualTo("hello John Doe");
            
            // Triggering the task should start the child case instance
            cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            
            assertThat(cmmnRule.getCmmnHistoryService().createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .variableName("customResponse")
                    .singleResult().getValue()).isEqualTo("hello John Doe");
        } finally {
            removeAllDeployments();
        }
    }

    // --Helper methods
    // ----------------------------------------------------------

    private void removeAllDeployments() {
        for (CmmnDeployment deployment : cmmnRule.getCmmnRepositoryService().createDeploymentQuery().list()) {
            cmmnRule.getCmmnRepositoryService().deleteDeployment(deployment.getId(), true);
        }
    }

}