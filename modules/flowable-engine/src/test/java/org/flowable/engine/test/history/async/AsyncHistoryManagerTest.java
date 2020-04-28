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
package org.flowable.engine.test.history.async;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.doReturn;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.async.AsyncHistoryManager;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.impl.persistence.entity.CommentEntityImpl;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.node.ObjectNode;

class AsyncHistoryManagerTest extends CustomConfigurationFlowableTestCase {

    private AsyncHistoryManager spiedAsyncHistoryManager;
    private AsyncHistorySession mockedAsyncHistorySession;
    private ArgumentCaptor<ObjectNode> dataCaptor = ArgumentCaptor.forClass(ObjectNode.class);

    public AsyncHistoryManagerTest() {
        super(AsyncHistoryManagerTest.class.getName());
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        spiedAsyncHistoryManager = new AsyncHistoryManager(processEngineConfiguration, HistoryLevel.ACTIVITY, false);
        spiedAsyncHistoryManager = Mockito.spy(spiedAsyncHistoryManager);
        processEngineConfiguration.setAsyncHistoryEnabled(true);
        processEngineConfiguration.setAsyncHistoryExecutorActivate(true);
        processEngineConfiguration.setHistoryManager(spiedAsyncHistoryManager);

        mockedAsyncHistorySession = Mockito.mock(AsyncHistorySession.class);
        doReturn(mockedAsyncHistorySession).when(spiedAsyncHistoryManager).getAsyncHistorySession();
    }

    @Test
    void createComment() {
        CommentEntity commentEntity = getCommentInstance(null);

        spiedAsyncHistoryManager.createComment(commentEntity);

        verify(mockedAsyncHistorySession, times(1))
                .addHistoricData(any(), eq(HistoryJsonConstants.TYPE_COMMENT_CREATED), dataCaptor.capture());

        checkResult(dataCaptor.getValue(), commentEntity);
    }

    @Test
    void updateComment() {
        CommentEntity commentEntity = getCommentInstance("id");

        spiedAsyncHistoryManager.updateComment(commentEntity);

        verify(mockedAsyncHistorySession, times(1))
                .addHistoricData(any(), eq(HistoryJsonConstants.TYPE_COMMENT_UPDATED), dataCaptor.capture());

        checkResult(dataCaptor.getValue(), commentEntity);
    }

    private CommentEntity getCommentInstance(String id) {
        CommentEntity commentEntity = new CommentEntityImpl();
        commentEntity.setId(id);
        commentEntity.setProcessInstanceId("processInstanceId");
        commentEntity.setTaskId("taskId");
        commentEntity.setType("type");
        commentEntity.setAction("action");
        commentEntity.setUserId("userId");
        commentEntity.setMessage("message");
        commentEntity.setFullMessage("fullMessage");
        return commentEntity;

    }
    private void checkResult(ObjectNode result, CommentEntity expected) {
        if(expected.getId() != null) {
            assertEquals(result.get(HistoryJsonConstants.ID).textValue(), expected.getId());
        }
        assertEquals(result.get(HistoryJsonConstants.PROCESS_INSTANCE_ID).textValue(), expected.getProcessInstanceId());
        assertEquals(result.get(HistoryJsonConstants.TASK_ID).textValue(), expected.getTaskId());
        assertEquals(result.get(HistoryJsonConstants.TYPE).textValue(), expected.getType());
        assertEquals(result.get(HistoryJsonConstants.ACTION).textValue(), expected.getAction());
        assertEquals(result.get(HistoryJsonConstants.USER_ID).textValue(), expected.getUserId());
        assertEquals(result.get(HistoryJsonConstants.MESSAGE).textValue(), expected.getMessage());
        assertEquals(result.get(HistoryJsonConstants.FULL_MESSAGE).textValue(), expected.getFullMessage());
    }
}