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
public class DeleteIdentityLinkCmd extends NeedsAppDefinitionCmd<Void> {

    private static final long serialVersionUID = 1L;

    public static int IDENTITY_USER = 1;
    public static int IDENTITY_GROUP = 2;

    protected String userId;

    protected String groupId;

    protected String type;

    public DeleteIdentityLinkCmd(String appDefinitionId, String userId, String groupId, String type) {
        super(appDefinitionId);
        validateParams(userId, groupId, type, appDefinitionId);
        this.appDefinitionId = appDefinitionId;
        this.userId = userId;
        this.groupId = groupId;
        this.type = type;
    }

    protected void validateParams(String userId, String groupId, String type, String taskId) {
        if (appDefinitionId == null) {
            throw new FlowableIllegalArgumentException("appDefinitionId is null");
        }

        if (type == null) {
            throw new FlowableIllegalArgumentException("type is required when adding a new app identity link");
        }

        if (userId == null && groupId == null) {
            throw new FlowableIllegalArgumentException("userId and groupId cannot both be null");
        }
    }

    @Override
    protected Void execute(CommandContext commandContext, AppDefinition appDefinition) {
        CommandContextUtil.getIdentityLinkService().deleteScopeIdentityLink(appDefinitionId, ScopeTypes.APP, userId, groupId, type);

        return null;
    }

}
