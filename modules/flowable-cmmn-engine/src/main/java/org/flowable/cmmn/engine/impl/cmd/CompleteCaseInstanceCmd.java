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

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Joram Barrez
 */
public class CompleteCaseInstanceCmd extends AbstractNeedsCaseInstanceCmd {

    public CompleteCaseInstanceCmd(String caseInstanceId) {
        super(caseInstanceId);
    }
    
    @Override
    protected void internalExecute(CommandContext commandContext, CaseInstanceEntity caseInstanceEntity) {
        if (!caseInstanceEntity.isCompleteable()) {
            throw new FlowableIllegalArgumentException("Can only complete a case instance which is marked as completeable. Check if there are active plan item instances.");
        }
        CommandContextUtil.getAgenda(commandContext).planCompleteCaseInstanceOperation(caseInstanceEntity);
    }

}
