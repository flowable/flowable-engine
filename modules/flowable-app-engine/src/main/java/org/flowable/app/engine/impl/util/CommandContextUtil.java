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
package org.flowable.app.engine.impl.util;

import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntityManager;
import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntityManager;
import org.flowable.app.engine.impl.persistence.entity.AppResourceEntityManager;
import org.flowable.app.engine.impl.persistence.entity.data.TableDataManager;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.identitylink.service.IdentityLinkService;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class CommandContextUtil {

    public static final String ATTRIBUTE_INVOLVED_CASE_INSTANCE_IDS = "ctx.attribute.involvedCaseInstanceIds";

    public static AppEngineConfiguration getAppEngineConfiguration() {
        return getAppEngineConfiguration(getCommandContext());
    }

    public static AppEngineConfiguration getAppEngineConfiguration(CommandContext commandContext) {
        return (AppEngineConfiguration) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_APP_ENGINE_CONFIG);
    }

    public static AppRepositoryService getAppRepositoryService() {
        return getAppEngineConfiguration().getAppRepositoryService();
    }

    public static ExpressionManager getExpressionManager() {
        return getExpressionManager(getCommandContext());
    }

    public static ExpressionManager getExpressionManager(CommandContext commandContext) {
        return getAppEngineConfiguration(commandContext).getExpressionManager();
    }
    
    public static FlowableEventDispatcher getEventDispatcher() {
        return getEventDispatcher(getCommandContext());
    }
    
    public static FlowableEventDispatcher getEventDispatcher(CommandContext commandContext) {
        return getAppEngineConfiguration(commandContext).getEventDispatcher();
    }

    public static AppDeploymentEntityManager getAppDeploymentEntityManager() {
        return getAppDeploymentEntityManager(getCommandContext());
    }

    public static AppDeploymentEntityManager getAppDeploymentEntityManager(CommandContext commandContext) {
        return getAppEngineConfiguration(commandContext).getAppDeploymentEntityManager();
    }

    public static AppResourceEntityManager getAppResourceEntityManager() {
        return getAppResourceEntityManager(getCommandContext());
    }

    public static AppResourceEntityManager getAppResourceEntityManager(CommandContext commandContext) {
        return getAppEngineConfiguration(commandContext).getAppResourceEntityManager();
    }

    public static AppDefinitionEntityManager getAppDefinitionEntityManager() {
        return getAppDefinitionEntityManager(getCommandContext());
    }

    public static AppDefinitionEntityManager getAppDefinitionEntityManager(CommandContext commandContext) {
        return getAppEngineConfiguration(commandContext).getAppDefinitionEntityManager();
    }

    public static TableDataManager getTableDataManager() {
        return getTableDataManager(getCommandContext());
    }

    public static TableDataManager getTableDataManager(CommandContext commandContext) {
        return getAppEngineConfiguration(commandContext).getTableDataManager();
    }

    public static VariableService getVariableService() {
        return getVariableService(getCommandContext());
    }

    public static VariableService getVariableService(CommandContext commandContext) {
        VariableService variableService = null;
        VariableServiceConfiguration variableServiceConfiguration = getVariableServiceConfiguration(commandContext);
        if (variableServiceConfiguration != null) {
            variableService = variableServiceConfiguration.getVariableService();
        }
        return variableService;
    }

    // IDM ENGINE

    public static IdmEngineConfiguration getIdmEngineConfiguration() {
        return getIdmEngineConfiguration(getCommandContext());
    }

    public static IdmEngineConfiguration getIdmEngineConfiguration(CommandContext commandContext) {
        return (IdmEngineConfiguration) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
    }

    public static IdmIdentityService getIdmIdentityService() {
        IdmIdentityService identityService = null;
        IdmEngineConfiguration idmEngineConfiguration = getIdmEngineConfiguration();
        if (idmEngineConfiguration != null) {
            identityService = idmEngineConfiguration.getIdmIdentityService();
        }

        return identityService;
    }

    public static IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return getIdentityLinkServiceConfiguration(getCommandContext());
    }

    public static IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration(CommandContext commandContext) {
        return (IdentityLinkServiceConfiguration) commandContext.getCurrentEngineConfiguration().getServiceConfigurations()
                        .get(EngineConfigurationConstants.KEY_IDENTITY_LINK_SERVICE_CONFIG);
    }

    public static IdentityLinkService getIdentityLinkService() {
        return getIdentityLinkService(getCommandContext());
    }

    public static IdentityLinkService getIdentityLinkService(CommandContext commandContext) {
        return getIdentityLinkServiceConfiguration(commandContext).getIdentityLinkService();
    }
    
    public static VariableServiceConfiguration getVariableServiceConfiguration() {
        return getVariableServiceConfiguration(getCommandContext());
    }

    public static VariableServiceConfiguration getVariableServiceConfiguration(CommandContext commandContext) {
        return (VariableServiceConfiguration) commandContext.getCurrentEngineConfiguration().getServiceConfigurations()
                        .get(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG);
    }

    public static DbSqlSession getDbSqlSession() {
        return getDbSqlSession(getCommandContext());
    }

    public static DbSqlSession getDbSqlSession(CommandContext commandContext) {
        return commandContext.getSession(DbSqlSession.class);
    }

    public static EntityCache getEntityCache() {
        return getEntityCache(getCommandContext());
    }

    public static EntityCache getEntityCache(CommandContext commandContext) {
        return commandContext.getSession(EntityCache.class);
    }

    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
