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
package org.flowable.engine.test.history;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.anyString;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.DefaultHistoryManager;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Joram Barrez
 */
public class HistoryManagerInvocationsTest extends CustomConfigurationFlowableTestCase {

    private HistoryManager spiedHistoryManager;

    public HistoryManagerInvocationsTest() {
        super(HistoryManagerInvocationsTest.class.getName());
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        HistoryManager historyManager = new DefaultHistoryManager(processEngineConfiguration, HistoryLevel.ACTIVITY, false);
        spiedHistoryManager = Mockito.spy(historyManager);
        processEngineConfiguration.setHistoryManager(spiedHistoryManager);
    }

    @Test
    public void testSingleTaskCreateAndComplete() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionId(deployOneTaskTestProcess()).start();
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        verify(spiedHistoryManager, times(1)).recordTaskCreated(any(), any());
        verify(spiedHistoryManager, times(1)).recordTaskEnd(any(), any(), any(), any());

        verify(spiedHistoryManager, times(1)).recordProcessInstanceStart(any());
        verify(spiedHistoryManager, times(1)).recordProcessInstanceEnd(any(), any(), any(), any());

    }

    @Test
    public void testCreateComment() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionId(deployOneTaskTestProcess()).start();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.addComment(task.getId(), processInstance.getProcessInstanceId(), "message");

        verify(spiedHistoryManager, times(1)).createComment(any(CommentEntity.class));
    }

    @Test
    public void testUpdateComment() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionId(deployOneTaskTestProcess()).start();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        Comment comment = taskService.addComment(task.getId(), processInstance.getProcessInstanceId(), "message");

        taskService.saveComment(comment);

        verify(spiedHistoryManager, times(1)).updateComment(any(CommentEntity.class));
    }

    @Test
    public void testDeleteComment() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionId(deployOneTaskTestProcess()).start();

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        Comment comment = taskService.addComment(task.getId(), processInstance.getProcessInstanceId(), "message");

        taskService.deleteComment(comment.getId());

        verify(spiedHistoryManager, times(1)).deleteComment(anyString());
    }
}
