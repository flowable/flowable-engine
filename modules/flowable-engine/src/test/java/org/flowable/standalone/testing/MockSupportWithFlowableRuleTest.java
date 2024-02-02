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
package org.flowable.standalone.testing;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableRule;
import org.flowable.engine.test.mock.FlowableMockSupport;
import org.flowable.engine.test.mock.MockServiceTask;
import org.flowable.engine.test.mock.MockServiceTasks;
import org.flowable.engine.test.mock.NoOpServiceTasks;
import org.flowable.standalone.testing.helpers.ServiceTaskTestMock;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class MockSupportWithFlowableRuleTest {

    @Rule
    public FlowableRule flowableRule = new FlowableRule() {

        @Override
        protected void configureProcessEngine() {
            ServiceTaskTestMock.CALL_COUNT.set(0);

            flowableRule.mockSupport().mockServiceTaskWithClassDelegate("com.yourcompany.delegate", ServiceTaskTestMock.class);
            flowableRule.mockSupport()
                    .mockServiceTaskWithClassDelegate("com.yourcompany.anotherDelegate", "org.flowable.standalone.testing.helpers.ServiceTaskTestMock");
        }

    };

    @Test
    @Deployment
    public void testClassDelegateMockSupport() {
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testClassDelegateStringMockSupport() {
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isEqualTo(1);
    }

    @Test
    @Deployment
    @MockServiceTask(originalClassName = "com.yourcompany.delegate", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock")
    public void testMockedServiceTaskAnnotation() {
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isEqualTo(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/testing/MockSupportWithFlowableRuleTest.testMockedServiceTaskAnnotation.bpmn20.xml" })
    @MockServiceTask(id = "serviceTask", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock")
    public void testMockedServiceTaskByIdAnnotation() {
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isEqualTo(1);
    }

    @Test
    @Deployment
    @MockServiceTasks({
            @MockServiceTask(originalClassName = "com.yourcompany.delegate1", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock"),
            @MockServiceTask(originalClassName = "com.yourcompany.delegate2", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock") })
    public void testMockedServiceTasksAnnotation() {
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isEqualTo(2);
    }

    @Test
    @Deployment
    @NoOpServiceTasks
    public void testNoOpServiceTasksAnnotation() {
        assertThat(flowableRule.mockSupport().getNrOfNoOpServiceTaskExecutions()).isZero();
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        assertThat(flowableRule.mockSupport().getNrOfNoOpServiceTaskExecutions()).isEqualTo(5);

        for (int i = 1; i <= 5; i++) {
            assertThat(flowableRule.mockSupport().getExecutedNoOpServiceTaskDelegateClassNames().get(i - 1)).isEqualTo("com.yourcompany.delegate" + i);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/testing/MockSupportWithFlowableRuleTest.testNoOpServiceTasksAnnotation.bpmn20.xml" })
    @NoOpServiceTasks(ids = { "serviceTask1", "serviceTask3", "serviceTask5" }, classNames = { "com.yourcompany.delegate2", "com.yourcompany.delegate4" })
    public void testNoOpServiceTasksWithIdsAnnotation() {
        FlowableMockSupport mockSupport = flowableRule.getMockSupport();
        assertThat(mockSupport.getNrOfNoOpServiceTaskExecutions()).isZero();
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        assertThat(mockSupport.getNrOfNoOpServiceTaskExecutions()).isEqualTo(5);

        for (int i = 1; i <= 5; i++) {
            assertThat(mockSupport.getExecutedNoOpServiceTaskDelegateClassNames().get(i - 1)).isEqualTo("com.yourcompany.delegate" + i);
        }
    }

}
