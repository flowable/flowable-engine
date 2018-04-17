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
package org.flowable.identitylink.service.impl.util;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityManager;

public class CommandContextUtil {

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
    
    public static DbSqlSession getDbSqlSession() {
        return getDbSqlSession(getCommandContext());
    }
    
    public static DbSqlSession getDbSqlSession(CommandContext commandContext) {
        return commandContext.getSession(DbSqlSession.class);
    }
    
    public static IdentityLinkEntityManager getIdentityLinkEntityManager() {
        return getIdentityLinkEntityManager(getCommandContext());
    }
    
    public static IdentityLinkEntityManager getIdentityLinkEntityManager(CommandContext commandContext) {
        return getIdentityLinkServiceConfiguration(commandContext).getIdentityLinkEntityManager();
    }
    
    public static HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager() {
        return getHistoricIdentityLinkEntityManager(getCommandContext());
    }
    
    public static HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager(CommandContext commandContext) {
        return getIdentityLinkServiceConfiguration(commandContext).getHistoricIdentityLinkEntityManager();
    }
    
    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
