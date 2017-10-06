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
package org.flowable.engine.impl.persistence.entity.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.impl.persistence.entity.CommentEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractProcessDataManager;
import org.flowable.engine.impl.persistence.entity.data.CommentDataManager;
import org.flowable.engine.task.Comment;
import org.flowable.engine.task.Event;

/**
 * @author Joram Barrez
 */
public class MybatisCommentDataManager extends AbstractProcessDataManager<CommentEntity> implements CommentDataManager {

    public MybatisCommentDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends CommentEntity> getManagedEntityClass() {
        return CommentEntityImpl.class;
    }

    @Override
    public CommentEntity create() {
        return new CommentEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Comment> findCommentsByTaskId(String taskId) {
        return getDbSqlSession().selectList("selectCommentsByTaskId", taskId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Comment> findCommentsByTaskIdAndType(String taskId, String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("taskId", taskId);
        params.put("type", type);
        return getDbSqlSession().selectListWithRawParameter("selectCommentsByTaskIdAndType", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Comment> findCommentsByType(String type) {
        return getDbSqlSession().selectList("selectCommentsByType", type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Event> findEventsByTaskId(String taskId) {
        return getDbSqlSession().selectList("selectEventsByTaskId", taskId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Event> findEventsByProcessInstanceId(String processInstanceId) {
        return getDbSqlSession().selectList("selectEventsByProcessInstanceId", processInstanceId);
    }

    @Override
    public void deleteCommentsByTaskId(String taskId) {
        getDbSqlSession().delete("deleteCommentsByTaskId", taskId, CommentEntityImpl.class);
    }

    @Override
    public void deleteCommentsByProcessInstanceId(String processInstanceId) {
        getDbSqlSession().delete("deleteCommentsByProcessInstanceId", processInstanceId, CommentEntityImpl.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Comment> findCommentsByProcessInstanceId(String processInstanceId) {
        return getDbSqlSession().selectList("selectCommentsByProcessInstanceId", processInstanceId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Comment> findCommentsByProcessInstanceId(String processInstanceId, String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("processInstanceId", processInstanceId);
        params.put("type", type);
        return getDbSqlSession().selectListWithRawParameter("selectCommentsByProcessInstanceIdAndType", params);
    }

    @Override
    public Comment findComment(String commentId) {
        return findById(commentId);
    }

    @Override
    public Event findEvent(String commentId) {
        return findById(commentId);
    }

}
