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
package org.flowable.app.engine.impl.cmd;

import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class AddIdentityLinkCmd extends NeedsAppDefinitionCmd<Void> {

    private static final long serialVersionUID = 1L;

    public static int IDENTITY_USER = 1;
    public static int IDENTITY_GROUP = 2;

    protected String identityId;

    protected int identityIdType;

    protected String identityType;

    public AddIdentityLinkCmd(String appDefinitionId, String identityId, int identityIdType, String identityType) {
        super(appDefinitionId);
        validateParams(appDefinitionId, identityId, identityIdType, identityType);
        this.appDefinitionId = appDefinitionId;
        this.identityId = identityId;
        this.identityIdType = identityIdType;
        this.identityType = identityType;
    }

    protected void validateParams(String appDefinitionId, String identityId, int identityIdType, String identityType) {
        if (appDefinitionId == null) {
            throw new FlowableIllegalArgumentException("appDefinitionId is null");
        }

        if (identityType == null) {
            throw new FlowableIllegalArgumentException("type is required when adding a new task identity link");
        }

        if (identityId == null) {
            throw new FlowableIllegalArgumentException("identityId is null");
        }

        if (identityIdType != IDENTITY_USER && identityIdType != IDENTITY_GROUP) {
            throw new FlowableIllegalArgumentException("identityIdType allowed values are 1 and 2");
        }
    }

    @Override
    protected Void execute(CommandContext commandContext, AppDefinition appDefinition) {

        if (IDENTITY_USER == identityIdType) {
            CommandContextUtil.getIdentityLinkService().createScopeIdentityLink(appDefinition.getId(), null, ScopeTypes.APP,
                            identityId, null, identityType);
            
        } else if (IDENTITY_GROUP == identityIdType) {
            CommandContextUtil.getIdentityLinkService().createScopeIdentityLink(appDefinition.getId(), null, ScopeTypes.APP,
                            null, identityId, identityType);
        }

        return null;
    }

}
