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
        assertEquals(202, resultServiceTask1.intValue());

        Integer resultServiceTask2 = (Integer) variables.get("resultServiceTask2");
        assertEquals(579, resultServiceTask2.intValue());

        // Verify only one instance was created
        assertEquals(1, SingletonDelegateExpressionBean.INSTANCE_COUNT.get());
    }

    @Test
    @Deployment
    public void testDelegateExpressionWithPrototypeBean() {
        runtimeService.startProcessInstanceByKey("delegateExpressionPrototype", CollectionUtil.singletonMap("input", 100));
        Task task = taskService.createTaskQuery().singleResult();
        Map<String, Object> variables = taskService.getVariables(task.getId());

        Integer resultServiceTask1 = (Integer) variables.get("resultServiceTask1");
        assertEquals(202, resultServiceTask1.intValue());

        Integer resultServiceTask2 = (Integer) variables.get("resultServiceTask2");
        assertEquals(579, resultServiceTask2.intValue());

        // Verify TWO INSTANCES were created
        assertEquals(2, PrototypeDelegateExpressionBean.INSTANCE_COUNT.get());
    }

}
