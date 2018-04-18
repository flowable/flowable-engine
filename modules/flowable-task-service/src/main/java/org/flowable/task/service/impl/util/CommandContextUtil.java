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
package org.flowable.task.service.impl.util;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityManager;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManager;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityManager;

public class CommandContextUtil {

    public static TaskServiceConfiguration getTaskServiceConfiguration() {
        return getTaskServiceConfiguration(getCommandContext());
    }
    
    public static TaskServiceConfiguration getTaskServiceConfiguration(CommandContext commandContext) {
        if (commandContext != null) {
            return (TaskServiceConfiguration) commandContext.getCurrentEngineConfiguration().getServiceConfigurations()
                            .get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        }
        return null;
    }
    
    public static VariableServiceConfiguration getVariableServiceConfiguration() {
        return getVariableServiceConfiguration(getCommandContext());
    }
    
    public static VariableServiceConfiguration getVariableServiceConfiguration(CommandContext commandContext) {
        if (commandContext != null) {
            return (VariableServiceConfiguration) commandContext.getCurrentEngineConfiguration().getServiceConfigurations()
                            .get(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG);
        }
        return null;
    }
    
    public static IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return getIdentityLinkServiceConfiguration(getCommandContext());
    }
    
    public static IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration(CommandContext commandContext) {
        if (commandContext != null) {
            return (IdentityLinkServiceConfiguration) commandContext.getCurrentEngineConfiguration().getServiceConfigurations()
                            .get(EngineConfigurationConstants.KEY_IDENTITY_LINK_SERVICE_CONFIG);
        }
        return null;
    }
    
    public static HistoricIdentityLinkService getHistoricIdentityLinkService() {
        return getHistoricIdentityLinkService(getCommandContext());
    }
    
    public static HistoricIdentityLinkService getHistoricIdentityLinkService(CommandContext commandContext) {
        return getIdentityLinkServiceConfiguration(commandContext).getHistoricIdentityLinkService();
    }
    
    public static DbSqlSession getDbSqlSession() {
        return getDbSqlSession(getCommandContext());
    }
    
    public static DbSqlSession getDbSqlSession(CommandContext commandContext) {
        return commandContext.getSession(DbSqlSession.class);
    }
    
    public static TaskEntityManager getTaskEntityManager() {
        return getTaskEntityManager(getCommandContext());
    }
    
    public static TaskEntityManager getTaskEntityManager(CommandContext commandContext) {
        return getTaskServiceConfiguration(commandContext).getTaskEntityManager();
    }
    
    public static HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager() {
        return getHistoricTaskInstanceEntityManager(getCommandContext());
    }
    
    public static HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager(CommandContext commandContext) {
        return getTaskServiceConfiguration(commandContext).getHistoricTaskInstanceEntityManager();
    }
    
    public static IdentityLinkEntityManager getIdentityLinkEntityManager() {
        return getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
    }
    
    public static HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager() {
        return getIdentityLinkServiceConfiguration().getHistoricIdentityLinkEntityManager();
    }
    
    public static VariableInstanceEntityManager getVariableInstanceEntityManager() {
        return getVariableServiceConfiguration().getVariableInstanceEntityManager();
    }
    
    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
