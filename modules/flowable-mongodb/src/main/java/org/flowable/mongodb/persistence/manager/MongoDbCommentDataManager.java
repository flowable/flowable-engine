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
package org.flowable.mongodb.persistence.manager;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.impl.persistence.entity.CommentEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.CommentDataManager;
import org.flowable.engine.task.Comment;
import org.flowable.engine.task.Event;

import com.mongodb.BasicDBObject;

/**
 * @author Tijs Rademakers
 */
public class MongoDbCommentDataManager extends AbstractMongoDbDataManager<CommentEntity> implements CommentDataManager {

    public static final String COLLECTION_COMMENTS = "comments";
    
    @Override
    public String getCollection() {
        return COLLECTION_COMMENTS;
    }

    @Override
    public CommentEntity create() {
        return new CommentEntityImpl();
    }
    
    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        return null;
    }

    @Override
    public List<Comment> findCommentsByTaskId(String taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Comment> findCommentsByTaskIdAndType(String taskId, String type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Comment> findCommentsByType(String type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Event> findEventsByTaskId(String taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Event> findEventsByProcessInstanceId(String processInstanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteCommentsByTaskId(String taskId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteCommentsByProcessInstanceId(String processInstanceId) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public List<Comment> findCommentsByProcessInstanceId(String processInstanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Comment> findCommentsByProcessInstanceId(String processInstanceId, String type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Comment findComment(String commentId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Event findEvent(String commentId) {
        // TODO Auto-generated method stub
        return null;
    }
    
    
}
