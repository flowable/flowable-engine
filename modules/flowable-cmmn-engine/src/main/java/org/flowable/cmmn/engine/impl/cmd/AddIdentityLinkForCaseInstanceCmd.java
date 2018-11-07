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

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.impl.util.IdentityLinkUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class AddIdentityLinkForCaseInstanceCmd implements Command<Void>, Serializable {

    private static final long serialVersionUID = 1L;

    protected String caseInstanceId;

    protected String userId;

    protected String groupId;

    protected String type;

    public AddIdentityLinkForCaseInstanceCmd(String caseInstanceId, String userId, String groupId, String type) {
        validateParams(caseInstanceId, userId, groupId, type);
        this.caseInstanceId = caseInstanceId;
        this.userId = userId;
        this.groupId = groupId;
        this.type = type;
    }

    protected void validateParams(String caseInstanceId, String userId, String groupId, String type) {

        if (caseInstanceId == null) {
            throw new FlowableIllegalArgumentException("caseInstanceId is null");
        }

        if (type == null) {
            throw new FlowableIllegalArgumentException("type is required when adding a new case instance identity link");
        }

        if (userId == null && groupId == null) {
            throw new FlowableIllegalArgumentException("userId and groupId cannot both be null");
        }

    }

    @Override
    public Void execute(CommandContext commandContext) {

        CaseInstanceEntityManager caseInstanceEntityManager = CommandContextUtil.getCaseInstanceEntityManager(commandContext);
        CaseInstanceEntity caseInstance = caseInstanceEntityManager.findById(caseInstanceId);

        if (caseInstance == null) {
            throw new FlowableObjectNotFoundException("Cannot find case instance with id " + caseInstanceId, CaseInstanceEntity.class);
        }

        IdentityLinkUtil.createCaseInstanceIdentityLink(caseInstance, userId, groupId, type);
        
        return null;

    }

}
