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
package org.flowable.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.history.HistoricTaskInstance;

public class AbstractProcessInstanceMigrationTest extends PluggableFlowableTestCase {

    protected void checkActivityInstances(ProcessDefinition processDefinition, ProcessInstance processInstance, String activityType,
            String... expectedActivityIds) {
        List<HistoricActivityInstance> historicTaskExecutions = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType(activityType)
                .list();
        assertThat(historicTaskExecutions)
                .extracting(HistoricActivityInstance::getActivityId)
                .containsExactlyInAnyOrder(expectedActivityIds);
        assertThat(historicTaskExecutions)
                .extracting(HistoricActivityInstance::getProcessDefinitionId)
                .containsOnly(processDefinition.getId());
        List<ActivityInstance> activityInstances = runtimeService.createActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityType(activityType)
                .list();
        if (runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count() == 1) {
            assertThat(activityInstances)
                    .extracting(ActivityInstance::getActivityId)
                    .containsExactlyInAnyOrder(expectedActivityIds);
            assertThat(activityInstances).extracting(ActivityInstance::getProcessDefinitionId).containsOnly(processDefinition.getId());
            activityInstances.forEach(
                    activityInstance -> {
                        HistoricActivityInstance historicActivityInstance = historyService.createHistoricActivityInstanceQuery()
                                .activityInstanceId(activityInstance.getId()).singleResult();
                        assertThat(activityInstance).isEqualToComparingFieldByField(historicActivityInstance);
                    }
            );
        } else {
            assertThat(activityInstances).isEmpty();
        }
    }

    protected void checkTaskInstance(ProcessDefinition processDefinition, ProcessInstance processInstance, String... expectedTaskDefinitionKeys) {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            List<HistoricTaskInstance> historicTasks = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
            assertThat(historicTasks)
                    .extracting(HistoricTaskInstance::getTaskDefinitionKey)
                    .containsExactlyInAnyOrder(expectedTaskDefinitionKeys);
            assertThat(historicTasks)
                    .extracting(HistoricTaskInstance::getProcessDefinitionId)
                    .containsOnly(processDefinition.getId());
        }
    }

}
