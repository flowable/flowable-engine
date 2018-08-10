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
package org.flowable.mongodb.persistence.mapper;

import org.bson.Document;
import org.flowable.engine.impl.persistence.entity.CommentEntityImpl;
import org.flowable.mongodb.persistence.EntityMapper;

/**
 * @author Tijs Rademakers
 */
public class CommentEntityMapper implements EntityMapper<CommentEntityImpl> {

    @Override
    public CommentEntityImpl fromDocument(Document document) {
        CommentEntityImpl commentEntity = new CommentEntityImpl();
        commentEntity.setId(document.getString("_id"));
        commentEntity.setAction(document.getString("action"));
        commentEntity.setFullMessage(document.getString("fullMessage"));
        commentEntity.setMessage(document.getString("message"));
        commentEntity.setProcessInstanceId(document.getString("processInstanceId"));
        commentEntity.setTaskId(document.getString("taskId"));
        commentEntity.setTime(document.getDate("time"));
        commentEntity.setType(document.getString("type"));
        commentEntity.setUserId(document.getString("userId"));
        
        return commentEntity;
    }

    @Override
    public Document toDocument(CommentEntityImpl commentEntity) {
        Document commentDocument = new Document();
        commentDocument.append("_id", commentEntity.getId());
        commentDocument.append("action", commentEntity.getAction());
        commentDocument.append("fullMessage", commentEntity.getFullMessage());
        commentDocument.append("message", commentEntity.getMessage());
        commentDocument.append("processInstanceId", commentEntity.getProcessInstanceId());
        commentDocument.append("taskId", commentEntity.getTaskId());
        commentDocument.append("time", commentEntity.getTime());
        commentDocument.append("type", commentEntity.getType());
        commentDocument.append("userId", commentEntity.getUserId());
        return commentDocument;
    }

}
