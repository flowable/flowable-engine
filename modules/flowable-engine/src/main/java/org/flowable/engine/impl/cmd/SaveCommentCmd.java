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

package org.flowable.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.impl.persistence.entity.CommentEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 */
public class SaveCommentCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    protected CommentEntity comment;

    public SaveCommentCmd(CommentEntity comment) {
        this.comment = comment;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (comment == null) {
            throw new FlowableIllegalArgumentException("comment is null");
        }
        if (comment.getId() == null) {
            throw new FlowableIllegalArgumentException("comment id is null");
        } 
        
        CommentEntityManager commentEntityManager = CommandContextUtil.getCommentEntityManager(commandContext);
        
        String eventMessage = comment.getFullMessage().replaceAll("\\s+", " ");
        if (eventMessage.length() > 163) {
            eventMessage = eventMessage.substring(0, 160) + "...";
        }
        comment.setMessage(eventMessage);

        commentEntityManager.update(comment);

        return null;
    }
}
