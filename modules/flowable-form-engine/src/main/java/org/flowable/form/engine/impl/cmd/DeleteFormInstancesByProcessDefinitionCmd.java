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
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormInstance;
import org.flowable.form.engine.impl.FormInstanceQueryImpl;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormResourceEntityManager;
import org.flowable.form.engine.impl.util.CommandContextUtil;

public class DeleteFormInstancesByProcessDefinitionCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String processDefinitionId;

    public DeleteFormInstancesByProcessDefinitionCmd(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (processDefinitionId == null) {
            throw new FlowableIllegalArgumentException("processDefinitionId is null");
        }

        FormInstanceEntityManager formInstanceEntityManager = CommandContextUtil.getFormInstanceEntityManager(commandContext);
        FormResourceEntityManager resourceEntityManager = CommandContextUtil.getResourceEntityManager(commandContext);
        FormInstanceQueryImpl formInstanceQuery = new FormInstanceQueryImpl(commandContext);
        formInstanceQuery.processDefinitionId(processDefinitionId);
        List<FormInstance> formInstances = formInstanceEntityManager.findFormInstancesByQueryCriteria(formInstanceQuery);
        for (FormInstance formInstance : formInstances) {
            if (formInstance.getFormValuesId() != null) {
                resourceEntityManager.delete(formInstance.getFormValuesId());
            }
        }
        
        formInstanceEntityManager.deleteFormInstancesByProcessDefinitionId(processDefinitionId);

        return null;
    }
}
