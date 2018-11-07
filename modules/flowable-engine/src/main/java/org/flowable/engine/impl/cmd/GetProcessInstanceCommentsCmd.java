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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.task.Comment;

/**
 * @author Tom Baeyens
 */
public class GetProcessInstanceCommentsCmd implements Command<List<Comment>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String processInstanceId;
    protected String type;

    public GetProcessInstanceCommentsCmd(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public GetProcessInstanceCommentsCmd(String processInstanceId, String type) {
        this.processInstanceId = processInstanceId;
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Comment> execute(CommandContext commandContext) {
        if (StringUtils.isNotBlank(type)) {
            List<Comment> commentsByProcessInstanceId = CommandContextUtil.getCommentEntityManager(commandContext).findCommentsByProcessInstanceId(processInstanceId, type);
            return commentsByProcessInstanceId;
        } else {
            return CommandContextUtil.getCommentEntityManager(commandContext).findCommentsByProcessInstanceId(processInstanceId);
        }
    }
}
