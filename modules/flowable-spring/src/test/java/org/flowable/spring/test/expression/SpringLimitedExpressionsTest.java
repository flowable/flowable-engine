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

package org.flowable.spring.test.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test limiting the exposed beans in expressions.
 *
 * @author Frederik Heremans
 */
@ContextConfiguration("classpath:org/flowable/spring/test/expression/expressionLimitedBeans-context.xml")
public class SpringLimitedExpressionsTest extends SpringFlowableTestCase {

    @Test
    @Deployment
    public void testLimitedBeansExposed() throws Exception {
        // Start process, which has a service-task which calls 'bean1', which is exposed
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("limitedExpressionProcess");

        String beanOutput = (String) runtimeService.getVariable(processInstance.getId(), "beanOutput");
        assertThat(beanOutput).isEqualTo("Activiti BPMN 2.0 process engine");

        // Finish the task, should continue to serviceTask which uses a bean that is present
        // in application-context, but not exposed explicitly in "beans", should throw error!
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(task).isNotNull();

        Consumer<FlowableException> flowableExceptionRequirements = flowableException -> {
            assertThat(flowableException.getCause().getMessage()).contains("Cannot resolve identifier 'bean2'");
        };
        assertThatThrownBy(() -> taskService.complete(task.getId()))
                .isInstanceOfSatisfying(FlowableException.class, flowableExceptionRequirements);
    }
}
