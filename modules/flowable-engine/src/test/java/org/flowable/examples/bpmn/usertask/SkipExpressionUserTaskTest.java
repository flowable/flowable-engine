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
package org.flowable.examples.bpmn.usertask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.history.HistoricTaskInstance;

public class SkipExpressionUserTaskTest extends PluggableFlowableTestCase {

    @Deployment
    public void test() {
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("skipExpressionUserTask");
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        taskService.complete(tasks.get(0).getId());
        assertEquals(0, taskService.createTaskQuery().list().size());

        Map<String, Object> variables2 = new HashMap<>();
        variables2.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables2.put("skip", false);
        ProcessInstance pi2 = runtimeService.startProcessInstanceByKey("skipExpressionUserTask", variables2);
        List<org.flowable.task.api.Task> tasks2 = taskService.createTaskQuery().list();
        assertEquals(1, tasks2.size());
        taskService.complete(tasks2.get(0).getId());
        assertEquals(0, taskService.createTaskQuery().list().size());

        Map<String, Object> variables3 = new HashMap<>();
        variables3.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables3.put("skip", true);
        ProcessInstance pi3 = runtimeService.startProcessInstanceByKey("skipExpressionUserTask", variables3);
        List<org.flowable.task.api.Task> tasks3 = taskService.createTaskQuery().list();
        assertEquals(0, tasks3.size());
    }

    @Deployment
    public void testWithCandidateGroups() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        vars.put("skip", true);
        runtimeService.startProcessInstanceByKey("skipExpressionUserTask", vars);
        assertEquals(0, taskService.createTaskQuery().list().size());
    }

    @Deployment
    public void testSkipMultipleTasks() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables.put("skip1", true);
        variables.put("skip2", true);
        variables.put("skip3", false);

        runtimeService.startProcessInstanceByKey("skipExpressionUserTask-testSkipMultipleTasks", variables);
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        assertEquals("Task3", tasks.get(0).getName());
    }
    
    @Deployment
    public void testEvents() {
        SkipFlowableEventListener eventListener = new SkipFlowableEventListener();
        processEngine.getRuntimeService().addEventListener(eventListener);
        
        Map<String, Object> variables2 = new HashMap<>();
        variables2.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables2.put("skip", false);
        runtimeService.startProcessInstanceByKey("skipExpressionUserTask", variables2);
        assertEquals(1, eventListener.getCreatedEvents().size());
        assertEquals(0, eventListener.getCompletedEvents().size());
        List<org.flowable.task.api.Task> tasks = taskService.createTaskQuery().list();
        assertEquals(1, tasks.size());
        taskService.complete(tasks.get(0).getId());
        assertEquals(1, eventListener.getCompletedEvents().size());
        assertEquals(0, taskService.createTaskQuery().list().size());
        
        eventListener.clearEvents();

        Map<String, Object> variables3 = new HashMap<>();
        variables3.put("_ACTIVITI_SKIP_EXPRESSION_ENABLED", true);
        variables3.put("skip", true);
        ProcessInstance skipPi = runtimeService.startProcessInstanceByKey("skipExpressionUserTask", variables3);
        assertEquals(0, eventListener.getCreatedEvents().size());
        assertEquals(0, eventListener.getCompletedEvents().size());
        tasks = taskService.createTaskQuery().list();
        assertEquals(0, tasks.size());
        
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            HistoricActivityInstance skipActivityInstance = historyService.createHistoricActivityInstanceQuery().processInstanceId(skipPi.getId())
                    .activityId("userTask1")
                    .singleResult();
            
            assertNotNull(skipActivityInstance);
            
            HistoricTaskInstance skipTaskInstance = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(skipPi.getId())
                    .singleResult();
            
            assertNotNull(skipTaskInstance);
        }
    }
    
    public class SkipFlowableEventListener extends AbstractFlowableEngineEventListener {

        public SkipFlowableEventListener() {
            super(new HashSet<>(Arrays.asList(FlowableEngineEventType.TASK_CREATED, FlowableEngineEventType.TASK_COMPLETED)));
        }
        protected List<FlowableEvent> createdEvents = new ArrayList<>();
        protected List<FlowableEvent> completedEvents = new ArrayList<>();

        @Override
        protected void taskCreated(FlowableEngineEntityEvent event) {
            createdEvents.add(event);
        }

        @Override
        protected void taskCompleted(FlowableEngineEntityEvent event) {
            completedEvents.add(event);
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }
        
        public List<FlowableEvent> getCreatedEvents() {
            return createdEvents;
        }

        public void setCreatedEvents(List<FlowableEvent> createdEvents) {
            this.createdEvents = createdEvents;
        }

        public List<FlowableEvent> getCompletedEvents() {
            return completedEvents;
        }

        public void setCompletedEvents(List<FlowableEvent> completedEvents) {
            this.completedEvents = completedEvents;
        }

        public void clearEvents() {
            this.createdEvents.clear();
            this.completedEvents.clear();
        }
    }
}
