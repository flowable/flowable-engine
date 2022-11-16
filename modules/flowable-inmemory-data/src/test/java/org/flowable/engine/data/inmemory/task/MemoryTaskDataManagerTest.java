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
package org.flowable.engine.data.inmemory.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;

import org.flowable.engine.data.inmemory.MemoryDataManagerFlowableTestCase;
import org.flowable.engine.data.inmemory.impl.task.MemoryTaskDataManager;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.impl.TaskQueryImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryTaskDataManagerTest extends MemoryDataManagerFlowableTestCase {

    @Test
    public void testFindTask() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/userTaskExecution.bpmn20.xml").deploy();

        MemoryTaskDataManager taskManager = getTaskDataManager();
        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("userTaskExecution");
            List<TaskEntity> tasks = taskManager.findTasksByProcessInstanceId(instance.getProcessInstanceId());
            assertThat(tasks).hasSize(1);
            assertThat(taskManager.findById(tasks.get(0).getId())).isNotNull();
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryTask() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/userTaskExecution.bpmn20.xml").deploy();

        MemoryTaskDataManager taskManager = getTaskDataManager();
        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("userTaskExecution");
            TaskQueryImpl query = Mockito.spy(query());
            assertThat(taskManager.findTasksWithRelatedEntitiesByQueryCriteria(query.processInstanceId(instance.getProcessInstanceId()))).hasSize(1);

            assertQueryMethods(TaskQueryImpl.class, query,
                            // not relevant to queries
                            "getId", "getLocale", "isOrActive", "isWithLocalizationFallback",
                            // not relevant outside sql
                            "getSafeCandidateGroups", "getSafeInvolvedGroups",
                            // only called when candidate user or group set
                            "isIgnoreAssigneeValue", "isBothCandidateAndAssigned", "getUserIdForCandidateAndAssignee",
                            // isUnassigned
                            "getUnassigned",
                            // getDelegationState
                            "getDelegationStateString",
                            // isNoDelegationState
                            "getNoDelegationState");
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testNativeQueryThrows() {
        try {
            MemoryTaskDataManager taskManager = getTaskDataManager();
            assertThatThrownBy(() -> taskManager.findTasksByNativeQuery(new HashMap<>())).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> taskManager.findTaskCountByNativeQuery(new HashMap<>())).isInstanceOf(IllegalStateException.class);
        } finally {
            processEngine.close();
        }
    }

    private TaskQueryImpl query() {
        return (TaskQueryImpl) getConfig().getTaskService().createTaskQuery();
    }
}
