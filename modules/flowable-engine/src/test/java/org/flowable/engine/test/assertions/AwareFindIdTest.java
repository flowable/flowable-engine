package org.flowable.engine.test.assertions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.flowable.engine.assertions.BpmnAwareTests.findId;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class AwareFindIdTest extends PluggableFlowableTestCase {

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findTest.bpmn20.xml")
        public void findPlainTaskByName() {
            String id = findId("Plain task");
            assertThat(id).isEqualTo("PlainTask_TestID");
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findTest.bpmn20.xml")
        public void findEndEventByName() {
            String end = findId("End");
            assertThat(end).isEqualTo("End_TestID");
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findTest.bpmn20.xml")
        public void findAttachedEventByName() {
            String attachedBoundaryEvent = findId("2 days");
            assertThat(attachedBoundaryEvent).isEqualTo("n2Days_TestID");
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findTest.bpmn20.xml")
        public void findGatewayByName() {
            String gateway = findId("Continue?");
            assertThat(gateway).isEqualTo("Continue_TestID");
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findTest.bpmn20.xml")
        public void nameNotFound() {
            assertThatThrownBy(() -> findId("This should not be found"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("doesn't exist");
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findTest.bpmn20.xml")
        public void testNameNull() {
            assertThatThrownBy(() -> findId(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findTest.bpmn20.xml")
        public void findAllElements() {
            String start = findId("Start");
            String plainTask = findId("Plain task");
            String userTask = findId("User task");
            String receiveTask = findId("Receive task");
            String attachedBoundaryEvent = findId("2 days");
            String gateway = findId("Continue?");
            String end = findId("End");
            String messageEnd = findId("Message End");

            assertThat(start).isEqualTo("Start_TestID");
            assertThat(plainTask).isEqualTo("PlainTask_TestID");
            assertThat(userTask).isEqualTo("UserTask_TestID");
            assertThat(receiveTask).isEqualTo("ReceiveTask_TestID");
            assertThat(attachedBoundaryEvent).isEqualTo("n2Days_TestID");
            assertThat(gateway).isEqualTo("Continue_TestID");
            assertThat(end).isEqualTo("End_TestID");
            assertThat(messageEnd).isEqualTo("MessageEnd_TestID");
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findInTwoPools.bpmn20.xml")
        public void findInTwoPoolsInPool1() {
            String callActivity = findId("Call activity one");
            assertThat(callActivity).isEqualTo("CallActivityOne_TestID");
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findInTwoPools.bpmn20.xml")
        public void findTwoPoolsInPool2() {
            String task = findId("Subprocess task");
            assertThat(task).isEqualTo("SubProcessTask_TestID");
        }

        @Test
        @Deployment(resources = {"org/flowable/engine/test/assertions/AwareFindIdTest.findTest.bpmn20.xml",
                "org/flowable/engine/test/assertions/AwareFindIdTest.findInTwoPools.bpmn20.xml"})
        public void findOneInEachOfTwoDiagrams() {
            String start = findId("Start");
            String plainTask = findId("Plain task");
            String startSuperProcess = findId("Super started");
            String taskTwo = findId("Task two");
            String proc2Started = findId("Proc 2 started");

            assertThat(start).isEqualTo("Start_TestID");
            assertThat(plainTask).isEqualTo("PlainTask_TestID");
            assertThat(startSuperProcess).isEqualTo("SuperStarted_TestID");
            assertThat(taskTwo).isEqualTo("TaskTwo_TestID");
            assertThat(proc2Started).isEqualTo("Proc2Started_TestID");
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findDuplicateNames.bpmn20.xml")
        public void processWithDuplicateNames() {
            assertThatThrownBy(() -> findId("Task one"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("not unique");

            assertThatThrownBy(() -> findId("Event one"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("not unique");

            assertThatThrownBy(() -> findId("Gateway one"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("not unique");
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findDuplicateNamesOnTaskAndGateway.bpmn20.xml")
        public void processWithDuplicateNamesOnDifferentElementsTypes() {
            assertThatThrownBy(() -> findId("Element one"))
                    .isInstanceOf(AssertionError.class)
                    .hasMessageContaining("not unique");
        }

        @Test
        @Deployment(resources = "org/flowable/engine/test/assertions/AwareFindIdTest.findDuplicateNamesOnTaskAndGateway.bpmn20.xml")
        public void processWithDuplicateNamesOnTaskAndGateway() {
            String startOne = findId("Start one");
            String endTwo = findId("End two");

            assertThat(startOne).isEqualTo("StartOne_TestID");
            assertThat(endTwo).isEqualTo("EndTwo_TestID");
        }
    }

