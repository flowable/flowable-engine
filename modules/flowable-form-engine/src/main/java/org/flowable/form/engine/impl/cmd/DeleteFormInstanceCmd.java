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
package org.flowable.form.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntity;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntityManager;
import org.flowable.form.engine.impl.util.CommandContextUtil;

public class DeleteFormInstanceCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String formInstanceId;

    public DeleteFormInstanceCmd(String formInstanceId) {
        this.formInstanceId = formInstanceId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (formInstanceId == null) {
            throw new FlowableIllegalArgumentException("formInstanceId is null");
        }

        FormInstanceEntityManager formInstanceEntityManager = CommandContextUtil.getFormInstanceEntityManager(commandContext);
        FormInstanceEntity formInstance = formInstanceEntityManager.findById(formInstanceId);
        if (formInstance == null) {
            throw new FlowableObjectNotFoundException("Form instance could not be found");
        }
        
        if (formInstance.getFormValuesId() != null) {
            CommandContextUtil.getResourceEntityManager(commandContext).delete(formInstance.getFormValuesId());
        }
        
        formInstanceEntityManager.delete(formInstance);

        return null;
    }
}
