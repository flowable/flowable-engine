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
package org.flowable.engine.impl.history.async.json.transformer;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDateFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.persistence.entity.CommentEntity;
import org.flowable.engine.impl.persistence.entity.CommentEntityImpl;
import org.flowable.engine.impl.persistence.entity.CommentEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class CommentUpdatedHistoryJsonTransformer extends AbstractHistoryJsonTransformer {
    @Override
    public List<String> getTypes() {
        return Collections.singletonList(HistoryJsonConstants.TYPE_COMMENT_UPDATED);
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        CommentEntityManager commentEntityManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getCommentEntityManager();

        CommentEntity commentEntity = new CommentEntityImpl();
        commentEntity.setId(getStringFromJson(historicalData, HistoryJsonConstants.ID));
        commentEntity.setProcessInstanceId(getStringFromJson(historicalData, HistoryJsonConstants.TYPE));
        commentEntity.setTime(getDateFromJson(historicalData, HistoryJsonConstants.TIME));
        commentEntity.setUserId(getStringFromJson(historicalData, HistoryJsonConstants.USER_ID));
        commentEntity.setTaskId(getStringFromJson(historicalData, HistoryJsonConstants.TASK_ID));
        commentEntity.setProcessInstanceId(getStringFromJson(historicalData, HistoryJsonConstants.PROCESS_INSTANCE_ID));
        commentEntity.setAction(getStringFromJson(historicalData, HistoryJsonConstants.ACTION));
        commentEntity.setMessage(getStringFromJson(historicalData, HistoryJsonConstants.MESSAGE));
        commentEntity.setFullMessage(getStringFromJson(historicalData, HistoryJsonConstants.FULL_MESSAGE));

        commentEntityManager.update(commentEntity);
    }
}
