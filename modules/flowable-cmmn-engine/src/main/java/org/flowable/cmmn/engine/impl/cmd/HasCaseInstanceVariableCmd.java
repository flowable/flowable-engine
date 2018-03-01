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
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class HasCaseInstanceVariableCmd implements Command<Boolean>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String caseInstanceId;
    protected String variableName;
    protected boolean isLocal;

    public HasCaseInstanceVariableCmd(String caseInstanceId, String variableName, boolean isLocal) {
        this.caseInstanceId = caseInstanceId;
        this.variableName = variableName;
        this.isLocal = isLocal;
    }

    @Override
    public Boolean execute(CommandContext commandContext) {
        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("caseInstanceId is null");
        }
        if (variableName == null) {
            throw new FlowableIllegalArgumentException("variableName is null");
        }

        CaseInstanceEntity caseInstance = CommandContextUtil.getCaseInstanceEntityManager(commandContext).findById(caseInstanceId);

        if (caseInstance == null) {
            throw new FlowableObjectNotFoundException("case instance " + caseInstanceId + " doesn't exist", CaseInstance.class);
        }

        boolean hasVariable = false;

        if (isLocal) {
            hasVariable = caseInstance.hasVariableLocal(variableName);
        } else {
            hasVariable = caseInstance.hasVariable(variableName);
        }

        return hasVariable;
    }
}
