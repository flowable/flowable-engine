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

import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableRule;
import org.flowable.engine.test.mock.FlowableMockSupport;
import org.flowable.engine.test.mock.MockServiceTask;
import org.flowable.engine.test.mock.MockServiceTasks;
import org.flowable.engine.test.mock.NoOpServiceTasks;
import org.flowable.standalone.testing.helpers.ServiceTaskTestMock;
import org.junit.Assert;
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
            flowableRule.mockSupport().mockServiceTaskWithClassDelegate("com.yourcompany.anotherDelegate", "org.flowable.standalone.testing.helpers.ServiceTaskTestMock");
        }

    };

    @Test
    @Deployment
    public void testClassDelegateMockSupport() {
        Assert.assertEquals(0, ServiceTaskTestMock.CALL_COUNT.get());
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        Assert.assertEquals(1, ServiceTaskTestMock.CALL_COUNT.get());
    }

    @Test
    @Deployment
    public void testClassDelegateStringMockSupport() {
        Assert.assertEquals(0, ServiceTaskTestMock.CALL_COUNT.get());
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        Assert.assertEquals(1, ServiceTaskTestMock.CALL_COUNT.get());
    }

    @Test
    @Deployment
    @MockServiceTask(originalClassName = "com.yourcompany.delegate", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock")
    public void testMockedServiceTaskAnnotation() {
        Assert.assertEquals(0, ServiceTaskTestMock.CALL_COUNT.get());
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        Assert.assertEquals(1, ServiceTaskTestMock.CALL_COUNT.get());
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/testing/MockSupportWithFlowableRuleTest.testMockedServiceTaskAnnotation.bpmn20.xml" })
    @MockServiceTask(id = "serviceTask", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock")
    public void testMockedServiceTaskByIdAnnotation() {
        Assert.assertEquals(0, ServiceTaskTestMock.CALL_COUNT.get());
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        Assert.assertEquals(1, ServiceTaskTestMock.CALL_COUNT.get());
    }

    @Test
    @Deployment
    @MockServiceTasks({ @MockServiceTask(originalClassName = "com.yourcompany.delegate1", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock"),
            @MockServiceTask(originalClassName = "com.yourcompany.delegate2", mockedClassName = "org.flowable.standalone.testing.helpers.ServiceTaskTestMock") })
    public void testMockedServiceTasksAnnotation() {
        Assert.assertEquals(0, ServiceTaskTestMock.CALL_COUNT.get());
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        Assert.assertEquals(2, ServiceTaskTestMock.CALL_COUNT.get());
    }

    @Test
    @Deployment
    @NoOpServiceTasks
    public void testNoOpServiceTasksAnnotation() {
        Assert.assertEquals(0, flowableRule.mockSupport().getNrOfNoOpServiceTaskExecutions());
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        Assert.assertEquals(5, flowableRule.mockSupport().getNrOfNoOpServiceTaskExecutions());

        for (int i = 1; i <= 5; i++) {
            Assert.assertEquals("com.yourcompany.delegate" + i, flowableRule.mockSupport().getExecutedNoOpServiceTaskDelegateClassNames().get(i - 1));
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/testing/MockSupportWithFlowableRuleTest.testNoOpServiceTasksAnnotation.bpmn20.xml" })
    @NoOpServiceTasks(ids = { "serviceTask1", "serviceTask3", "serviceTask5" }, classNames = { "com.yourcompany.delegate2", "com.yourcompany.delegate4" })
    public void testNoOpServiceTasksWithIdsAnnotation() {
        FlowableMockSupport mockSupport = flowableRule.getMockSupport();
        Assert.assertEquals(0, mockSupport.getNrOfNoOpServiceTaskExecutions());
        flowableRule.getRuntimeService().startProcessInstanceByKey("mockSupportTest");
        Assert.assertEquals(5, mockSupport.getNrOfNoOpServiceTaskExecutions());

        for (int i = 1; i <= 5; i++) {
            Assert.assertEquals("com.yourcompany.delegate" + i, mockSupport.getExecutedNoOpServiceTaskDelegateClassNames().get(i - 1));
        }
    }

}
