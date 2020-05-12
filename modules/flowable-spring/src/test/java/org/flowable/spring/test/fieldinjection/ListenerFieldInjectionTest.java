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
package org.flowable.spring.test.fieldinjection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Joram Barrez
 */
@ContextConfiguration("classpath:org/flowable/spring/test/fieldinjection/fieldInjectionSpringTest-context.xml")
public class ListenerFieldInjectionTest extends SpringFlowableTestCase {

    @Test
    @Deployment
    public void testDelegateExpressionListenerFieldInjection() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("listenerFieldInjection", CollectionUtil.singletonMap("startValue", 42));

        // Process start execution listener
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        Map<String, Object> variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables).hasSize(2);
        assertThat(((Number) variables.get("processStartValue")).intValue()).isEqualTo(4200);

        // Sequence flow execution listener
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables).hasSize(3);
        assertThat(((Number) variables.get("sequenceFlowValue")).intValue()).isEqualTo(420000);

        // task listeners
        taskService.complete(task.getId());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables).hasSize(4);
        assertThat(((Number) variables.get("taskCreateValue")).intValue()).isEqualTo(210000);

        taskService.complete(task.getId());
        variables = runtimeService.getVariables(processInstance.getId());
        assertThat(variables).hasSize(5);
        assertThat(((Number) variables.get("taskCompleteValue")).intValue()).isEqualTo(105000);

        assertThat(TestExecutionListener.INSTANCE_COUNT.get()).isEqualTo(1);
        assertThat(TestTaskListener.INSTANCE_COUNT.get()).isEqualTo(1);
    }

}
