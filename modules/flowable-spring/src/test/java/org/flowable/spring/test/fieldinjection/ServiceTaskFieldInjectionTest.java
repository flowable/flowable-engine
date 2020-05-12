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
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Joram Barrez
 */
@ContextConfiguration("classpath:org/flowable/spring/test/fieldinjection/fieldInjectionSpringTest-context.xml")
public class ServiceTaskFieldInjectionTest extends SpringFlowableTestCase {

    @Test
    @Deployment
    public void testDelegateExpressionWithSingletonBean() {
        runtimeService.startProcessInstanceByKey("delegateExpressionSingleton", CollectionUtil.singletonMap("input", 100));
        Task task = taskService.createTaskQuery().singleResult();
        Map<String, Object> variables = taskService.getVariables(task.getId());

        Integer resultServiceTask1 = (Integer) variables.get("resultServiceTask1");
        assertThat(resultServiceTask1.intValue()).isEqualTo(202);

        Integer resultServiceTask2 = (Integer) variables.get("resultServiceTask2");
        assertThat(resultServiceTask2.intValue()).isEqualTo(579);

        // Verify only one instance was created
        assertThat(SingletonDelegateExpressionBean.INSTANCE_COUNT.get()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testDelegateExpressionWithPrototypeBean() {
        runtimeService.startProcessInstanceByKey("delegateExpressionPrototype", CollectionUtil.singletonMap("input", 100));
        Task task = taskService.createTaskQuery().singleResult();
        Map<String, Object> variables = taskService.getVariables(task.getId());

        Integer resultServiceTask1 = (Integer) variables.get("resultServiceTask1");
        assertThat(resultServiceTask1.intValue()).isEqualTo(202);

        Integer resultServiceTask2 = (Integer) variables.get("resultServiceTask2");
        assertThat(resultServiceTask2.intValue()).isEqualTo(579);

        // Verify TWO INSTANCES were created
        assertThat(PrototypeDelegateExpressionBean.INSTANCE_COUNT.get()).isEqualTo(2);
    }

}
