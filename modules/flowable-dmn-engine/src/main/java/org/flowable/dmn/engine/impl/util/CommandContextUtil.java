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
package org.flowable.dmn.engine.impl.util;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionTableEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.TableDataManager;

public class CommandContextUtil {
    
    public static DmnEngineConfiguration getDmnEngineConfiguration() {
        return getDmnEngineConfiguration(getCommandContext());
    }
    
    public static DmnEngineConfiguration getDmnEngineConfiguration(CommandContext commandContext) {
        if (commandContext != null) {
            return (DmnEngineConfiguration) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_DMN_ENGINE_CONFIG);
        }
        return null;
    }
    
    public static DbSqlSession getDbSqlSession() {
        return getDbSqlSession(getCommandContext());
    }
    
    public static DbSqlSession getDbSqlSession(CommandContext commandContext) {
        return commandContext.getSession(DbSqlSession.class);
    }
    
    public static TableDataManager getTableDataManager() {
        return getTableDataManager(getCommandContext());
    }
    
    public static TableDataManager getTableDataManager(CommandContext commandContext) {
        return getDmnEngineConfiguration(commandContext).getTableDataManager();
    }
    
    public static DmnResourceEntityManager getResourceEntityManager() {
        return getResourceEntityManager(getCommandContext());
    }
    
    public static DmnResourceEntityManager getResourceEntityManager(CommandContext commandContext) {
        return getDmnEngineConfiguration(commandContext).getResourceEntityManager();
    }
    
    public static DmnDeploymentEntityManager getDeploymentEntityManager() {
        return getDeploymentEntityManager(getCommandContext());
    }
    
    public static DmnDeploymentEntityManager getDeploymentEntityManager(CommandContext commandContext) {
        return getDmnEngineConfiguration(commandContext).getDeploymentEntityManager();
    }
    
    public static DecisionTableEntityManager getDecisionTableEntityManager() {
        return getDecisionTableEntityManager(getCommandContext());
    }
    
    public static DecisionTableEntityManager getDecisionTableEntityManager(CommandContext commandContext) {
        return getDmnEngineConfiguration(commandContext).getDecisionTableEntityManager();
    }
    
    public static HistoricDecisionExecutionEntityManager getHistoricDecisionExecutionEntityManager() {
        return getHistoricDecisionExecutionEntityManager(getCommandContext());
    }
    
    public static HistoricDecisionExecutionEntityManager getHistoricDecisionExecutionEntityManager(CommandContext commandContext) {
        return getDmnEngineConfiguration(commandContext).getHistoricDecisionExecutionEntityManager();
    }
    
    public static DmnRepositoryService getDmnRepositoryService() {
        return getDmnRepositoryService(getCommandContext());
    }
    
    public static DmnRepositoryService getDmnRepositoryService(CommandContext commandContext) {
        return getDmnEngineConfiguration(commandContext).getDmnRepositoryService();
    }
    
    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
