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
package org.flowable.engine.data.inmemory.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;

import org.flowable.engine.data.inmemory.MemoryDataManagerFlowableTestCase;
import org.flowable.engine.data.inmemory.impl.activity.MemoryActivityInstanceDataManager;
import org.flowable.engine.impl.ActivityInstanceQueryImpl;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryActivityDataManagerTest extends MemoryDataManagerFlowableTestCase {

    @Test
    public void testFindActivity() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        MemoryActivityInstanceDataManager actManager = getActivityInstanceDataManager();
        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution");
            ActivityInstanceEntity activity = waitForActivity(instance.getProcessInstanceId(), "service1");
            assertThat(activity).isNotNull();

            assertThat(actManager.findById(activity.getId())).isNotNull();
            assertThat(actManager.findActivityInstancesByExecutionIdAndActivityId(activity.getExecutionId(), "service1")).hasSize(1);
            assertThat(actManager.findActivityInstancesByExecutionIdAndActivityId(activity.getExecutionId(), "foo")).isEmpty();
            assertThat(actManager.findUnfinishedActivityInstancesByExecutionAndActivityId(activity.getExecutionId(), "service1")).hasSize(1);
            assertThat(actManager.findUnfinishedActivityInstancesByExecutionAndActivityId(activity.getExecutionId(), "foo")).isEmpty();
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testFindActivityAsync() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableAsyncExecution.bpmn20.xml").deploy();
        processEngine.getProcessEngineConfiguration().getAsyncExecutor().start();

        MemoryActivityInstanceDataManager actManager = getActivityInstanceDataManager();
        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableAsyncExecution");
            ActivityInstanceEntity activity = waitForActivity(instance.getProcessInstanceId(), "service1");
            assertThat(activity).isNotNull();

            assertThat(actManager.findById(activity.getId())).isNotNull();
            assertThat(actManager.findActivityInstancesByExecutionIdAndActivityId(activity.getExecutionId(), "service1")).hasSize(1);
            assertThat(actManager.findActivityInstancesByExecutionIdAndActivityId(activity.getExecutionId(), "foo")).isEmpty();
            assertThat(actManager.findUnfinishedActivityInstancesByExecutionAndActivityId(activity.getExecutionId(), "service1")).hasSize(1);
            assertThat(actManager.findUnfinishedActivityInstancesByExecutionAndActivityId(activity.getExecutionId(), "foo")).isEmpty();
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryActivity() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        MemoryActivityInstanceDataManager actManager = getActivityInstanceDataManager();
        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution");
            ActivityInstanceEntity activity = waitForActivity(instance.getProcessInstanceId(), "service1");
            assertThat(activity).isNotNull();

            ActivityInstanceQueryImpl query = Mockito.spy(query());
            assertThat(actManager.findActivityInstancesByQueryCriteria(query.activityId(activity.getActivityId()))).hasSize(1);
            assertQueryMethods(ActivityInstanceQueryImpl.class, query);

            assertThat(actManager.findActivityInstancesByQueryCriteria(query().processInstanceId(instance.getProcessInstanceId()).unfinished())).hasSize(1);
            assertThat(actManager.findActivityInstancesByQueryCriteria(query().processInstanceId(instance.getProcessInstanceId()).finished())).hasSize(2);
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testNativeQueryThrows() {
        try {
            MemoryActivityInstanceDataManager actManager = getActivityInstanceDataManager();
            assertThatThrownBy(() -> actManager.findActivityInstanceCountByNativeQuery(new HashMap<>())).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> actManager.findActivityInstancesByNativeQuery(new HashMap<>())).isInstanceOf(IllegalStateException.class);
        } finally {
            processEngine.close();
        }
    }

    private ActivityInstanceQueryImpl query() {
        return (ActivityInstanceQueryImpl) processEngine.getRuntimeService().createActivityInstanceQuery();
    }
}
