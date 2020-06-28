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
package org.flowable.cdi.test.api.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cdi.BusinessProcess;
import org.flowable.cdi.impl.annotation.CompleteTaskInterceptor;
import org.flowable.cdi.test.CdiFlowableTestCase;
import org.flowable.cdi.test.impl.beans.DeclarativeProcessController;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * Testcase for assuring that the {@link CompleteTaskInterceptor} works as expected
 * 
 * @author Daniel Meyer
 */
public class CompleteTaskTest extends CdiFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/cdi/test/api/annotation/CompleteTaskTest.bpmn20.xml")
    public void testCompleteTask() {

        BusinessProcess businessProcess = getBeanInstance(BusinessProcess.class);

        businessProcess.startProcessByKey("keyOfTheProcess");

        Task task = taskService.createTaskQuery().singleResult();

        // associate current unit of work with the task:
        businessProcess.startTask(task.getId());

        getBeanInstance(DeclarativeProcessController.class).completeTask();

        // assert that now the task is completed
        assertThat(taskService.createTaskQuery().singleResult()).isNull();
    }

}
