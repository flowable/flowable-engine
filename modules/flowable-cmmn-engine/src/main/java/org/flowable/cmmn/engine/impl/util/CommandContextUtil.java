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
package org.flowable.cmmn.engine.impl.util;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgenda;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityManager;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;

public class CommandContextUtil {
    
    public static CmmnEngineConfiguration getCmmnEngineConfiguration() {
        return getCmmnEngineConfiguration(getCommandContext());
    }
    
    public static CmmnEngineConfiguration getCmmnEngineConfiguration(CommandContext commandContext) {
        return (CmmnEngineConfiguration) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
    }
    
    public static CmmnDeploymentEntityManager getCmmnDeploymentEntityManager() {
        return getCmmnDeploymentEntityManager(getCommandContext());
    }
    
    public static CmmnDeploymentEntityManager getCmmnDeploymentEntityManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getCmmnDeploymentEntityManager();
    }
    
    public static CmmnResourceEntityManager getCmmnResourceEntityManager() {
        return getCmmnResourceEntityManager(getCommandContext());
    }
    
    public static CmmnResourceEntityManager getCmmnResourceEntityManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getCmmnResourceEntityManager();
    }
    
    public static CaseDefinitionEntityManager getCaseDefinitionEntityManager() {
        return getCaseDefinitionEntityManager(getCommandContext());
    }
    
    public static CaseDefinitionEntityManager getCaseDefinitionEntityManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getCaseDefinitionEntityManager();
    }
    
    public static CmmnEngineAgenda getAgenda() {
        return getAgenda(getCommandContext());
    }
    
    public static CmmnEngineAgenda getAgenda(CommandContext commandContext) {
        return commandContext.getSession(CmmnEngineAgenda.class);
    }
    
    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
