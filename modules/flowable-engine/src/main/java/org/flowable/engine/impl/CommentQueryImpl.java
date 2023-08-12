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
package org.flowable.engine.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.query.AbstractQuery;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.task.Comment;
import org.flowable.engine.task.CommentQuery;

/**
 * @author David Lamas
 */
public class CommentQueryImpl extends AbstractQuery<CommentQuery, Comment> implements CommentQuery, Serializable {
    private ProcessEngineConfigurationImpl processEngineConfiguration;

    private String commentId;
    private String taskId;
    private String processInstanceId;
    private String type;
    private String createdBy;
    private Collection<String> createdByOneOf;
    private Date createdBefore;
    private Date createdAfter;
    private Date createdOn;

    public CommentQueryImpl() {
    }

    public CommentQueryImpl(CommandExecutor commandExecutor, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(commandExecutor);
        this.processEngineConfiguration = processEngineConfiguration;
    }

    public CommentQueryImpl(CommandContext commandContext, ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(commandContext);
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public CommentQuery commentId(String commentId) {
        this.commentId = commentId;
        return this;
    }

    @Override
    public CommentQuery taskId(String taskId) {
        this.taskId = taskId;
        return this;
    }

    @Override
    public CommentQuery processInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        return this;
    }

    @Override
    public CommentQuery type(String type) {
        this.type = type;
        return this;
    }

    @Override
    public CommentQuery createdBy(String userId) {
        if (userId != null) {
            if (createdByOneOf != null) {
                throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both createdBy and createdByOneOf");
            }
        }
        this.createdBy = userId;
        return this;
    }

    @Override
    public CommentQuery createdByOneOf(Collection<String> userIds) {
        if (userIds != null) {
            if (userIds.isEmpty()) {
                throw new FlowableIllegalArgumentException("User id list cannot be empty");
            }

            if (createdBy != null) {
                throw new FlowableIllegalArgumentException("Invalid query usage: cannot set both createdBy and createdByOneOf");
            }
        }

        this.createdByOneOf = userIds;
        return this;
    }

    @Override
    public CommentQuery createdBefore(Date beforeTime) {
        this.createdBefore = beforeTime;
        return this;
    }

    @Override
    public CommentQuery createdAfter(Date afterTime) {
        this.createdAfter = afterTime;
        return this;
    }

    @Override
    public CommentQuery createdOn(Date createTime) {
        this.createdOn = createTime;
        return this;
    }

    @Override
    public CommentQuery orderByCreateTime() {
        return orderBy(CommentQueryProperty.TIME);
    }

    @Override
    public CommentQuery orderByUser() {
        return orderBy(CommentQueryProperty.USER_ID);
    }

    @Override
    public CommentQuery orderByType() {
        return orderBy(CommentQueryProperty.TYPE);
    }

    @Override
    public CommentQuery orderByTaskId() {
        return orderBy(CommentQueryProperty.TASK_ID);
    }

    @Override
    public CommentQuery orderByProcessInstanceId() {
        return orderBy(CommentQueryProperty.PROCESS_INSTANCE_ID);
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        return processEngineConfiguration.getCommentEntityManager().findCommentCountByQueryCriteria(this);
    }

    @Override
    public List<Comment> executeList(CommandContext commandContext) {
        return processEngineConfiguration.getCommentEntityManager().findCommentsByQueryCriteria(this);
    }
}
