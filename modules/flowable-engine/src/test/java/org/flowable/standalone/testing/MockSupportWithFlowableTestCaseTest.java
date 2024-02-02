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
import org.flowable.engine.test.FlowableTestCase;
import org.flowable.engine.test.mock.MockServiceTask;
import org.flowable.engine.test.mock.MockServiceTasks;
import org.flowable.engine.test.mock.NoOpServiceTasks;
import org.flowable.standalone.testing.helpers.ServiceTaskTestMock;

/**
 * @author Joram Barrez
 */
public class MockSupportWithFlowableTestCaseTest extends FlowableTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        ServiceTaskTestMock.CALL_COUNT.set(0);

        mockSupport().mockServiceTaskWithClassDelegate("com.yourcompany.delegate", ServiceTaskTestMock.class);
        mockSupport().mockServiceTaskWithClassDelegate("com.yourcompany.anotherDelegate", "org.flowable.standalone.testing.helpers.ServiceTaskTestMock");
    }

    @Deployment
    public void testClassDelegateMockSupport() {
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();
        runtimeService.startProcessInstanceByKey("mockSupportTest");
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isEqualTo(1);
    }

    @Deployment
    public void testClassDelegateStringMockSupport() {
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();
        runtimeService.startProcessInstanceByKey("mockSupportTest");
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isEqualTo(1);
    }

    @Deployment
    @MockServiceTask(originalClassName = "com.yourcompany.delegate", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock")
    public void testMockedServiceTaskAnnotation() {
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();
        runtimeService.startProcessInstanceByKey("mockSupportTest");
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isEqualTo(1);
    }

    @Deployment(resources = { "org/flowable/standalone/testing/MockSupportWithFlowableTestCaseTest.testMockedServiceTaskAnnotation.bpmn20.xml" })
    @MockServiceTask(id = "serviceTask", mockedClassName = "org.activiti.standalone.testing.helpers.ServiceTaskTestMock")
    public void testMockedServiceTaskByIdAnnotation() {
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();
        runtimeService.startProcessInstanceByKey("mockSupportTest");
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isEqualTo(1);
    }

    @Deployment
    @MockServiceTasks({
            @MockServiceTask(originalClassName = "com.yourcompany.delegate1", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock"),
            @MockServiceTask(originalClassName = "com.yourcompany.delegate2", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock") })
    public void testMockedServiceTasksAnnotation() {
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isZero();
        runtimeService.startProcessInstanceByKey("mockSupportTest");
        assertThat(ServiceTaskTestMock.CALL_COUNT.get()).isEqualTo(2);
    }

    @Deployment
    @NoOpServiceTasks
    public void testNoOpServiceTasksAnnotation() {
        assertThat(mockSupport().getNrOfNoOpServiceTaskExecutions()).isZero();
        runtimeService.startProcessInstanceByKey("mockSupportTest");
        assertThat(mockSupport().getNrOfNoOpServiceTaskExecutions()).isEqualTo(5);

        for (int i = 1; i <= 5; i++) {
            assertThat(mockSupport().getExecutedNoOpServiceTaskDelegateClassNames().get(i - 1)).isEqualTo("com.yourcompany.delegate" + i);
        }
    }

    @Deployment(resources = { "org/flowable/standalone/testing/MockSupportWithFlowableTestCaseTest.testNoOpServiceTasksAnnotation.bpmn20.xml" })
    @NoOpServiceTasks(ids = { "serviceTask1", "serviceTask3", "serviceTask5" }, classNames = { "com.yourcompany.delegate2", "com.yourcompany.delegate4" })
    public void testNoOpServiceTasksWithIdsAnnotation() {
        assertThat(mockSupport().getNrOfNoOpServiceTaskExecutions()).isZero();
        runtimeService.startProcessInstanceByKey("mockSupportTest");
        assertThat(mockSupport().getNrOfNoOpServiceTaskExecutions()).isEqualTo(5);

        for (int i = 1; i <= 5; i++) {
            assertThat(mockSupport().getExecutedNoOpServiceTaskDelegateClassNames().get(i - 1)).isEqualTo("com.yourcompany.delegate" + i);
        }
    }

}
