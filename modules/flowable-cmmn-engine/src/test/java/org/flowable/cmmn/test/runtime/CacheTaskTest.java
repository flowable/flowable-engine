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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

public class CacheTaskTest extends FlowableCmmnTestCase {
    
    @Test
    @CmmnDeployment
    public void testCaseCache() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .start();
        
        HistoricPlanItemInstance planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).singleResult();
        
        assertNotNull(ServiceCacheTask.caseInstanceId);
        assertEquals(caseInstance.getId(), ServiceCacheTask.caseInstanceId);
        assertNotNull(ServiceCacheTask.historicCaseInstanceId);
        assertEquals(caseInstance.getId(), ServiceCacheTask.historicCaseInstanceId);
        assertNotNull(ServiceCacheTask.planItemInstanceId);
        assertEquals(planItemInstance.getId(), ServiceCacheTask.planItemInstanceId);
        assertNotNull(ServiceCacheTask.historicPlanItemInstanceId);
        assertEquals(planItemInstance.getId(), ServiceCacheTask.historicPlanItemInstanceId);
    }
    
    @Test
    @CmmnDeployment
    public void testTaskListenerCache() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(task);

        assertNotNull(CacheTaskListener.taskId);
        assertEquals(task.getId(), CacheTaskListener.taskId);
        assertNotNull(CacheTaskListener.historicTaskId);
        assertEquals(task.getId(), CacheTaskListener.historicTaskId);
    }
    
    @Test
    @CmmnDeployment
    public void testMilestoneListenerCache() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .start();
        HistoricMilestoneInstance milestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstance.getId()).singleResult();
        assertNotNull(milestoneInstance);

        assertNotNull(CacheMilestoneListener.milestoneInstanceId);
        assertEquals(milestoneInstance.getId(), CacheMilestoneListener.milestoneInstanceId);
        assertNotNull(CacheMilestoneListener.historicMilestoneInstanceId);
        assertEquals(milestoneInstance.getId(), CacheMilestoneListener.historicMilestoneInstanceId);
    }
}
