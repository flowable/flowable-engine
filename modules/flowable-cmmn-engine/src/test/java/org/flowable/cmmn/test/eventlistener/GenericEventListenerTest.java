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
package org.flowable.cmmn.test.eventlistener;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.junit.Test;

public class GenericEventListenerTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testWithRepetitionMultiple() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetition")
                .variable("keepRepeating", true)
                .start();

        PlanItemInstance eventInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER)
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(eventInstance.getId());
        
        cmmnRuntimeService.setVariable(caseInstance.getId(), "keepRepeating", false);

        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskDefinitionKey("taskA").active().singleResult().getId());
        
        eventInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER)
                .singleResult();
        cmmnRuntimeService.triggerPlanItemInstance(eventInstance.getId());
        
        assertCaseInstanceEnded(caseInstance);
    }
    
    @Test
    @CmmnDeployment
    public void testWithRepetitionParallel() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testRepetition")
                .variable("keepRepeating", true)
                .start();

        cmmnEngineConfiguration.getCommandExecutor().execute(new Command<Void>() {

            @Override
            public Void execute(CommandContext commandContext) {
                PlanItemInstance eventInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER)
                        .singleResult();
                cmmnRuntimeService.triggerPlanItemInstance(eventInstance.getId());
                
                cmmnRuntimeService.setVariable(caseInstance.getId(), "keepRepeating", false);
                
                eventInstance = cmmnRuntimeService.createPlanItemInstanceQuery()
                        .planItemDefinitionType(PlanItemDefinitionType.GENERIC_EVENT_LISTENER)
                        .singleResult();
                cmmnRuntimeService.triggerPlanItemInstance(eventInstance.getId());
                
                return null;
            }
            
        });
        
        cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskDefinitionKey("taskA").active().singleResult().getId());
        
        assertCaseInstanceEnded(caseInstance);
    }

}

