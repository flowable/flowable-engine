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
package org.flowable.examples.bpmn.tasklistener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Rich Kroll, Tijs Rademakers
 * @author Filip Hrisafov
 */
public class ScriptTaskListenerTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/examples/bpmn/tasklistener/ScriptTaskListenerTest.bpmn20.xml" })
    public void testScriptTaskListener() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("scriptTaskListenerProcess");
        org.flowable.task.api.Task task = taskService.createTaskQuery().singleResult();
        assertThat(task.getName()).as("Name does not match").isEqualTo("All your base are belong to us");

        taskService.complete(task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricTaskInstance historicTask = historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).singleResult();
            assertThat(historicTask.getOwner()).isEqualTo("kermit");

            task = taskService.createTaskQuery().singleResult();
            assertThat(task.getName()).as("Task name not set with 'bar' variable").isEqualTo("BAR");
        }

        Object bar = runtimeService.getVariable(processInstance.getId(), "bar");
        assertThat(bar).as("Expected 'bar' variable to be local to script").isNull();

        Object foo = runtimeService.getVariable(processInstance.getId(), "foo");
        assertThat(foo).as("Could not find the 'foo' variable in variable scope").isEqualTo("FOO");
    }

    @Test
    @Deployment
    public void testThrowFlowableIllegalArgumentException() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("scriptTaskListenerProcess"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("Illegal argument in listener");
    }

    @Test
    @Deployment
    public void testThrowNonFlowableException() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("scriptTaskListenerProcess"))
                .isInstanceOf(FlowableException.class)
                .hasMessage("problem evaluating script: java.lang.RuntimeException: Illegal argument in listener in <eval> at line number 2 at column number 7")
                .getRootCause()
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Illegal argument in listener");
    }

}
