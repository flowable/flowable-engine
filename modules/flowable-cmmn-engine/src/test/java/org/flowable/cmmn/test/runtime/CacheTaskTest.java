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
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;

import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class CacheTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testCaseCache() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();

        HistoricPlanItemInstance planItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                .planItemInstanceCaseInstanceId(caseInstance.getId()).singleResult();

        assertThat(ServiceCacheTask.caseInstanceId).isNotNull();
        assertThat(ServiceCacheTask.caseInstanceId).isEqualTo(caseInstance.getId());
        assertThat(ServiceCacheTask.historicCaseInstanceId).isNotNull();
        assertThat(ServiceCacheTask.historicCaseInstanceId).isEqualTo(caseInstance.getId());
        assertThat(ServiceCacheTask.planItemInstanceId).isNotNull();
        assertThat(ServiceCacheTask.planItemInstanceId).isEqualTo(planItemInstance.getId());
        assertThat(ServiceCacheTask.historicPlanItemInstanceId).isNotNull();
        assertThat(ServiceCacheTask.historicPlanItemInstanceId).isEqualTo(planItemInstance.getId());
    }

    @Test
    @CmmnDeployment
    public void testTaskListenerCache() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        assertThat(CacheTaskListener.taskId).isNotNull();
        assertThat(CacheTaskListener.taskId).isEqualTo(task.getId());
        assertThat(CacheTaskListener.historicTaskId).isNotNull();
        assertThat(CacheTaskListener.historicTaskId).isEqualTo(task.getId());
    }

    @Test
    @CmmnDeployment
    public void testMilestoneListenerCache() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("myCase")
                .start();
        HistoricMilestoneInstance milestoneInstance = cmmnHistoryService.createHistoricMilestoneInstanceQuery()
                .milestoneInstanceCaseInstanceId(caseInstance.getId()).singleResult();
        assertThat(milestoneInstance).isNotNull();

        assertThat(CacheMilestoneListener.milestoneInstanceId).isNotNull();
        assertThat(CacheMilestoneListener.milestoneInstanceId).isEqualTo(milestoneInstance.getId());
        assertThat(CacheMilestoneListener.historicMilestoneInstanceId).isNotNull();
        assertThat(CacheMilestoneListener.historicMilestoneInstanceId).isEqualTo(milestoneInstance.getId());
    }

    @Test
    @CmmnDeployment
    public void testCaseInstanceQueryWithIncludeVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testCaseInstanceQueryWithIncludeVariables")
                .variable("myVar1", "Hello")
                .variable("myVar2", "World")
                .variable("myVar3", 123)
                .start();

        Map.Entry[] entries = {
                entry("myVar1", "Hello"),
                entry("myVar2", "World"),
                entry("myVar3", 123),
                entry("varFromTheServiceTask", "valueFromTheServiceTask")
        };

        assertThat(caseInstance.getCaseVariables()).containsOnly(entries);
        assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.VARIABLES).containsOnly(entries);
        assertThat(TestQueryCaseInstanceWithIncludeVariablesDelegate.HISTORIC_VARIABLES).containsOnly(entries);
    }
}
