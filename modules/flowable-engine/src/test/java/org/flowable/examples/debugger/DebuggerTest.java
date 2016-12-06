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
package org.flowable.examples.debugger;

import org.apache.commons.collections.Predicate;
import org.flowable.engine.impl.agenda.ContinueProcessOperation;
import org.flowable.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.examples.groovy.GroovyScriptTest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * This example shows how to use Activiti in the debug mode
 *
 * @author martin.grofcik
 */
public class DebuggerTest extends ResourceFlowableTestCase {

    public DebuggerTest() {
        super("org/flowable/examples/debugger/DebuggerTest.cfg.xml");
    }

    /**
     * the following test is counterpart for {@link GroovyScriptTest#testSetVariableThroughExecutionInScript()} test
     */
    @Deployment(resources = "org/flowable/examples/groovy/GroovyScriptTest.testSetVariableThroughExecutionInScript.bpmn20.xml")
    public void testNoBreakPoint() {
        TestDebuggerImpl debugger = (TestDebuggerImpl) this.processEngineConfiguration.getBeans().get("testDebugger");
        debugger.setBreakPoint(new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                return false; // no break point at all
            }
        });

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("setScriptVariableThroughExecution");

        assertThat("Process has stopped on the first user task. Variable must exist.",
                runtimeService.hasVariable(pi.getId(), "myVar"), is(true));
        assertThat("Process instance has to be stopped at 'theTask'",
                runtimeService.createExecutionQuery().parentId(pi.getId()).singleResult().getActivityId(),
                is("theTask"));
        assertThat("There is exactly one MyTask task present", taskService.createTaskQuery().singleResult().getName(), is("my task"));
    }

    /**
     * the following test is counterpart for {@link GroovyScriptTest#testSetVariableThroughExecutionInScript()} test
     */
    @Deployment(resources = "org/flowable/examples/groovy/GroovyScriptTest.testSetVariableThroughExecutionInScript.bpmn20.xml")
    public void testBreak_SetVariableThroughExecutionInScript() {
        TestDebuggerImpl debugger = (TestDebuggerImpl) this.processEngineConfiguration.getBeans().get("testDebugger");
        debugger.setBreakPoint(new Predicate() {
            @Override
            public boolean evaluate(Object o) {
                if (o instanceof ContinueProcessOperation) {
                    ContinueProcessOperation operation = (ContinueProcessOperation) o;
                    return operation.getExecution().getActivityId().equals("theScriptTask");
                }
                return false;
            }
        });

        ProcessInstance pi = runtimeService.startProcessInstanceByKey("setScriptVariableThroughExecution");

        assertThat("Process has stopped on the first groovy task. Variable does not exist.",
                runtimeService.hasVariable(pi.getId(), "myVar"), is(false));
        assertThat("Process instance has to be stopped at 'theScriptTask'",
                runtimeService.createExecutionQuery().parentId(pi.getId()).singleResult().getActivityId(),
                is("theScriptTask"));
        assertThat("There is no task present", taskService.createTaskQuery().count(), is(0L));
    }

    /**
     * the following test is counterpart for {@link GroovyScriptTest#testSetVariableThroughExecutionInScript()} test
     */
    @Deployment(resources = "org/flowable/examples/groovy/GroovyScriptTest.testSetVariableThroughExecutionInScript.bpmn20.xml")
    public void testContinue_SetVariableThroughExecutionInScript() throws InterruptedException {
        testBreak_SetVariableThroughExecutionInScript();
        TestDebuggerImpl debugger = (TestDebuggerImpl) this.processEngineConfiguration.getBeans().get("testDebugger");
        CommandExecutor commandExecutor = this.processEngineConfiguration.getCommandExecutor();

        debugger.continueOperationExecution(commandExecutor, debugger.getAgenda());

        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processDefinitionKey("setScriptVariableThroughExecution").singleResult();
        assertThat("Process has passed through the first groovy task. Variable must exist.",
                runtimeService.hasVariable(pi.getId(), "myVar"), is(true));
        assertThat("There is exactly one MyTask task present", taskService.createTaskQuery().singleResult().getName(), is("my task"));
        assertThat("Process instance has to be stopped at 'theTask'",
                runtimeService.createExecutionQuery().parentId(pi.getId()).singleResult().getActivityId(),
                is("theTask"));
    }

}
