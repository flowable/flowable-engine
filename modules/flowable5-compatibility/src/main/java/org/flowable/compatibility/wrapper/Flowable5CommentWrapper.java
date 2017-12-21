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

package org.flowable.compatibility.wrapper;

import java.util.Date;

import org.activiti.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.task.Comment;

/**
 * Wraps an v5 comment to an v6 {@link Comment}.
 * 
 * @author Tijs Rademakers
 */
public class Flowable5CommentWrapper implements Comment {

    private org.activiti.engine.task.Comment activit5Comment;

    public Flowable5CommentWrapper(org.activiti.engine.task.Comment activit5Comment) {
        this.activit5Comment = activit5Comment;
    }

    @Override
    public String getId() {
        return activit5Comment.getId();
    }

    @Override
    public String getUserId() {
        return activit5Comment.getUserId();
    }

    @Override
    public Date getTime() {
        return activit5Comment.getTime();
    }

    @Override
    public String getTaskId() {
        return activit5Comment.getTaskId();
    }

    @Override
    public String getProcessInstanceId() {
        return activit5Comment.getProcessInstanceId();
    }

    @Override
    public String getType() {
        return activit5Comment.getType();
    }

    @Override
    public String getFullMessage() {
        return activit5Comment.getFullMessage();
    }

    public org.activiti.engine.task.Comment getRawObject() {
        return activit5Comment;
    }

    @Override
    public void setUserId(String userId) {
        ((CommentEntity) activit5Comment).setUserId(userId);
    }

    @Override
    public void setTime(Date time) {
        ((CommentEntity) activit5Comment).setTime(time);
    }

    @Override
    public void setTaskId(String taskId) {
        ((CommentEntity) activit5Comment).setTaskId(taskId);
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        ((CommentEntity) activit5Comment).setProcessInstanceId(processInstanceId);
    }

    @Override
    public void setType(String type) {
        ((CommentEntity) activit5Comment).setType(type);
    }

    @Override
    public void setFullMessage(String fullMessage) {
        ((CommentEntity) activit5Comment).setFullMessage(fullMessage);
    }
}
