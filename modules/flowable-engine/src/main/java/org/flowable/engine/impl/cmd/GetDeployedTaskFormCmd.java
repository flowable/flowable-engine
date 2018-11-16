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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.form.StartFormData;
import org.flowable.engine.form.TaskFormData;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

import java.io.InputStream;

/**
 * @author shareniu
 */
public class GetDeployedTaskFormCmd implements Command<InputStream> {

    protected String taskId;

    public GetDeployedTaskFormCmd(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public InputStream execute(CommandContext commandContext) {
        if (taskId == null) {
            throw new FlowableIllegalArgumentException("taskId is null");
        }
        TaskFormData taskFormData = new GetTaskFormCmd(taskId).execute(commandContext);
        String taskFormKey = taskFormData.getFormKey();
        if (taskFormKey == null) {
            throw new FlowableIllegalArgumentException("The task form key is not set");
        }
        InputStream inputStream = new GetDeploymentResourceCmd(taskFormData.getDeploymentId(), taskFormKey).execute(commandContext);
        return  inputStream;
    }


}
