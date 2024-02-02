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
package org.flowable.engine.test.api.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to comments.
 *
 * @author Frederik Heremans
 */
public class CommentEventsTest extends PluggableFlowableTestCase {

    private TestFlowableEntityEventListener listener;

    /**
     * Test create, update and delete events of comments on a task/process.
     */
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml" })
    public void testCommentEntityEvents() throws Exception {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            assertThat(task).isNotNull();

            // Create link-comment
            Comment comment = taskService.addComment(task.getId(), task.getProcessInstanceId(), "comment");
            assertThat(listener.getEventsReceived()).hasSize(2);
            FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
            assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
            assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
            assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
            Comment commentFromEvent = (Comment) event.getEntity();
            assertThat(commentFromEvent.getId()).isEqualTo(comment.getId());

            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
            listener.clearEventsReceived();

            // Finally, delete comment
            taskService.deleteComment(comment.getId());
            assertThat(listener.getEventsReceived()).hasSize(1);
            event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
            assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
            assertThat(event.getProcessInstanceId()).isEqualTo(processInstance.getId());
            assertThat(event.getExecutionId()).isEqualTo(processInstance.getId());
            assertThat(event.getProcessDefinitionId()).isEqualTo(processInstance.getProcessDefinitionId());
            commentFromEvent = (Comment) event.getEntity();
            assertThat(commentFromEvent.getId()).isEqualTo(comment.getId());
        }
    }

    @Test
    public void testCommentEntityEventsStandaloneTask() throws Exception {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.AUDIT, processEngineConfiguration)) {
            org.flowable.task.api.Task task = null;
            try {
                task = taskService.newTask();
                taskService.saveTask(task);
                assertThat(task).isNotNull();

                // Create link-comment
                Comment comment = taskService.addComment(task.getId(), null, "comment");
                assertThat(listener.getEventsReceived()).hasSize(2);
                FlowableEngineEntityEvent event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
                assertThat(event.getProcessInstanceId()).isNull();
                assertThat(event.getExecutionId()).isNull();
                assertThat(event.getProcessDefinitionId()).isNull();
                Comment commentFromEvent = (Comment) event.getEntity();
                assertThat(commentFromEvent.getId()).isEqualTo(comment.getId());

                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(1);
                assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_INITIALIZED);
                listener.clearEventsReceived();

                // Finally, delete comment
                taskService.deleteComment(comment.getId());
                assertThat(listener.getEventsReceived()).hasSize(1);
                event = (FlowableEngineEntityEvent) listener.getEventsReceived().get(0);
                assertThat(event.getType()).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);
                assertThat(event.getProcessInstanceId()).isNull();
                assertThat(event.getExecutionId()).isNull();
                assertThat(event.getProcessDefinitionId()).isNull();
                commentFromEvent = (Comment) event.getEntity();
                assertThat(commentFromEvent.getId()).isEqualTo(comment.getId());

            } finally {
                if (task != null && task.getId() != null) {
                    taskService.deleteTask(task.getId(), true);
                }
            }
        }
    }

    @BeforeEach
    protected void setUp() throws Exception {
        listener = new TestFlowableEntityEventListener(Comment.class);
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
