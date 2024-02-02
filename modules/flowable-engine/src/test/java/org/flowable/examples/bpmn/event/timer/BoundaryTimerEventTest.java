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
package org.flowable.examples.bpmn.event.timer;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class BoundaryTimerEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testInterruptingTimerDuration() {

        // Start process instance
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("escalationExample");

        // There should be one task, with a timer : first line support
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("First line support");

        // Manually execute the job
        Job timer = managementService.createTimerJobQuery().singleResult();
        managementService.moveTimerToExecutableJob(timer.getId());
        managementService.executeJob(timer.getId());

        // The timer has fired, and the second task (second line support) now exists
        task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        assertThat(task.getName()).isEqualTo("Handle escalated issue");
    }

}
