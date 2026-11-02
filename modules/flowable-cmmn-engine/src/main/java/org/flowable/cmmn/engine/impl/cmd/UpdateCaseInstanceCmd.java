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
package org.flowable.cmmn.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceUpdateBuilderImpl;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * {@link Command} that updates properties of an existing case instance.
 *
 * @author Tijs Rademakers
 */
public class UpdateCaseInstanceCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected CaseInstanceUpdateBuilderImpl builder;

    public UpdateCaseInstanceCmd(CaseInstanceUpdateBuilderImpl builder) {
        this.builder = builder;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        String caseInstanceId = builder.getCaseInstanceId();
        if (caseInstanceId == null || caseInstanceId.isEmpty()) {
            throw new FlowableIllegalArgumentException("The case instance id is mandatory, but '" + caseInstanceId + "' has been provided.");
        }

        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstanceEntity = caseInstanceEntityManager.findById(caseInstanceId);
        if (caseInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No case instance found for id = '" + caseInstanceId + "'.", CaseInstance.class);
        }

        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);

        if (builder.isBusinessKeySet()) {
            caseInstanceEntityManager.updateCaseInstanceBusinessKey(caseInstanceEntity, builder.getBusinessKey());
        }

        if (builder.isBusinessStatusSet()) {
            caseInstanceEntityManager.updateCaseInstanceBusinessStatus(caseInstanceEntity, builder.getBusinessStatus());
        }

        if (builder.isNameSet()) {
            caseInstanceEntity.setName(builder.getName());
            cmmnEngineConfiguration.getCmmnHistoryManager().recordUpdateCaseInstanceName(caseInstanceEntity, builder.getName());
        }

        if (builder.isDueDateSet()) {
            caseInstanceEntityManager.updateCaseInstanceDueDate(caseInstanceEntity, builder.getDueDate());
        }

        return null;
    }
}
