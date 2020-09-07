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

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.runtime.ChangePlanItemStateBuilderImpl;
import org.flowable.cmmn.engine.impl.runtime.CmmnDynamicStateManager;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class ChangePlanItemStateCmd implements Command<Void> {

    protected CmmnEngineConfiguration cmmnEngineConfiguration;
    
    protected ChangePlanItemStateBuilderImpl changePlanItemStateBuilder;

    public ChangePlanItemStateCmd(ChangePlanItemStateBuilderImpl changePlanItemStateBuilder, CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.changePlanItemStateBuilder = changePlanItemStateBuilder;
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        if (changePlanItemStateBuilder.getActivatePlanItemDefinitions().size() == 0 &&
                changePlanItemStateBuilder.getTerminatePlanItemDefinitions().size() == 0 &&
                changePlanItemStateBuilder.getChangeToAvailableStatePlanItemDefinitions().size() == 0) {
            
            throw new FlowableIllegalArgumentException("No move plan item instance or (activate) plan item definition ids provided");

        } else if (changePlanItemStateBuilder.getCaseInstanceId() == null) {
            throw new FlowableIllegalArgumentException("Case instance id is required");   
        }

        CmmnDynamicStateManager dynamicStateManager = cmmnEngineConfiguration.getDynamicStateManager();
        dynamicStateManager.movePlanItemInstanceState(changePlanItemStateBuilder, commandContext);

        return null;
    }
}
