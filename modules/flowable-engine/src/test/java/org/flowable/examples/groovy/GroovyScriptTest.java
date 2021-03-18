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

package org.flowable.examples.groovy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobQuery;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 */
public class GroovyScriptTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testScriptExecution() {
        int[] inputArray = new int[] { 1, 2, 3, 4, 5 };
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("scriptExecution", CollectionUtil.singletonMap("inputArray", inputArray));

        Integer result = (Integer) runtimeService.getVariable(pi.getId(), "sum");
        assertThat(result.intValue()).isEqualTo(15);
    }

    @Test
    @Deployment
    public void testSetVariableThroughExecutionInScript() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("setScriptVariableThroughExecution");

        // Since 'def' is used, the 'scriptVar' will be script local
        // and not automatically stored as a process variable.
        assertThat(runtimeService.getVariable(pi.getId(), "scriptVar")).isNull();
        assertThat(runtimeService.getVariable(pi.getId(), "myVar")).isEqualTo("test123");
    }

    @Test
    @Deployment
    public void testAsyncScript() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testAsyncScript");

        JobQuery jobQuery = managementService.createJobQuery().processInstanceId(processInstance.getId());
        List<Job> jobs = jobQuery.list();
        assertThat(jobs).hasSize(1);

        waitForJobExecutorToProcessAllJobs(7000L, 100L);
        assertThat(jobQuery.count()).isZero();

        assertProcessEnded(processInstance.getId());
    }

    @Test
    @Deployment
    public void testScriptThrowsFlowableIllegalArgumentException() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("scriptExecution"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("Illegal argument in listener");
    }

    @Test
    @Deployment
    public void testScriptThrowsNonFlowableException() {
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("scriptExecution"))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessage("problem evaluating script: javax.script.ScriptException: java.lang.RuntimeException: Illegal argument in listener")
                .getRootCause()
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Illegal argument in listener");
    }

}
