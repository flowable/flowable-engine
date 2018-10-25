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
package org.flowable.entitylink.service.impl.util;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntityManager;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntityManager;

public class CommandContextUtil {

    public static EntityLinkServiceConfiguration getEntityLinkServiceConfiguration() {
        return getEntityLinkServiceConfiguration(getCommandContext());
    }
    
    public static EntityLinkServiceConfiguration getEntityLinkServiceConfiguration(CommandContext commandContext) {
        if (commandContext != null) {
            return (EntityLinkServiceConfiguration) commandContext.getCurrentEngineConfiguration().getServiceConfigurations()
                            .get(EngineConfigurationConstants.KEY_ENTITY_LINK_SERVICE_CONFIG);
        }
        return null;
    }
    
    public static DbSqlSession getDbSqlSession() {
        return getDbSqlSession(getCommandContext());
    }
    
    public static DbSqlSession getDbSqlSession(CommandContext commandContext) {
        return commandContext.getSession(DbSqlSession.class);
    }
    
    public static EntityLinkEntityManager getEntityLinkEntityManager() {
        return getEntityLinkEntityManager(getCommandContext());
    }
    
    public static EntityLinkEntityManager getEntityLinkEntityManager(CommandContext commandContext) {
        return getEntityLinkServiceConfiguration(commandContext).getEntityLinkEntityManager();
    }
    
    public static HistoricEntityLinkEntityManager getHistoricEntityLinkEntityManager() {
        return getHistoricEntityLinkEntityManager(getCommandContext());
    }
    
    public static HistoricEntityLinkEntityManager getHistoricEntityLinkEntityManager(CommandContext commandContext) {
        return getEntityLinkServiceConfiguration(commandContext).getHistoricEntityLinkEntityManager();
    }
    
    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
