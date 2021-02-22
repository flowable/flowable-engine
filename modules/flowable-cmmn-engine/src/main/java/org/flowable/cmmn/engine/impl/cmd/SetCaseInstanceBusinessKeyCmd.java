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
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * {@link Command} that changes the business key of an existing case instance.
 *
 * @author Matthias Stöckli
 */
public class SetCaseInstanceBusinessKeyCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String caseInstanceId;
    private final String businessKey;

    public SetCaseInstanceBusinessKeyCmd(String caseInstanceId, String businessKey) {
        if (caseInstanceId == null || caseInstanceId.length() < 1) {
            throw new FlowableIllegalArgumentException("The case instance id is mandatory, but '" + caseInstanceId + "' has not been provided.");
        }
        if (businessKey == null) {
            throw new FlowableIllegalArgumentException("The business key is mandatory, but 'null' has been provided.");
        }

        this.caseInstanceId = caseInstanceId;
        this.businessKey = businessKey;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstanceEntity = caseInstanceEntityManager.findById(caseInstanceId);
        if (caseInstanceEntity == null) {
            throw new FlowableObjectNotFoundException("No case instance found for id = '" + caseInstanceId + "'.", CaseInstance.class);
        }

        caseInstanceEntityManager.updateCaseInstanceBusinessKey(caseInstanceEntity, businessKey);

        return null;
    }
}
