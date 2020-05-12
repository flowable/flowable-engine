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
package org.flowable.eventregistry.impl.util;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.entity.TableDataManager;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.ChannelDefinitionEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntityManager;

public class CommandContextUtil {

    public static EventRegistryEngineConfiguration getEventRegistryConfiguration() {
        return getEventRegistryConfiguration(getCommandContext());
    }
    
    public static EventRegistryEngineConfiguration getEventRegistryConfiguration(CommandContext commandContext) {
        if (commandContext != null) {
            return (EventRegistryEngineConfiguration) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        }
        return null;
    }
    
    public static EventRepositoryService getEventRepositoryService() {
        return getEventRegistryConfiguration().getEventRepositoryService();
    }
    
    public static DbSqlSession getDbSqlSession() {
        return getDbSqlSession(getCommandContext());
    }
    
    public static DbSqlSession getDbSqlSession(CommandContext commandContext) {
        return commandContext.getSession(DbSqlSession.class);
    }
    
    public static EventResourceEntityManager getResourceEntityManager() {
        return getResourceEntityManager(getCommandContext());
    }
    
    public static EventResourceEntityManager getResourceEntityManager(CommandContext commandContext) {
        return getEventRegistryConfiguration(commandContext).getResourceEntityManager();
    }
    
    public static EventDeploymentEntityManager getDeploymentEntityManager() {
        return getDeploymentEntityManager(getCommandContext());
    }
    
    public static EventDeploymentEntityManager getDeploymentEntityManager(CommandContext commandContext) {
        return getEventRegistryConfiguration(commandContext).getDeploymentEntityManager();
    }
    
    public static EventDefinitionEntityManager getEventDefinitionEntityManager() {
        return getEventDefinitionEntityManager(getCommandContext());
    }
    
    public static EventDefinitionEntityManager getEventDefinitionEntityManager(CommandContext commandContext) {
        return getEventRegistryConfiguration(commandContext).getEventDefinitionEntityManager();
    }
    
    public static ChannelDefinitionEntityManager getChannelDefinitionEntityManager() {
        return getChannelDefinitionEntityManager(getCommandContext());
    }
    
    public static ChannelDefinitionEntityManager getChannelDefinitionEntityManager(CommandContext commandContext) {
        return getEventRegistryConfiguration(commandContext).getChannelDefinitionEntityManager();
    }
    
    public static TableDataManager getTableDataManager() {
        return getTableDataManager(getCommandContext());
    }
    
    public static TableDataManager getTableDataManager(CommandContext commandContext) {
        return getEventRegistryConfiguration(commandContext).getTableDataManager();
    }
    
    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
