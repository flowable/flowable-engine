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
package org.flowable.form.engine.impl.util;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormInstanceEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormResourceEntityManager;
import org.flowable.form.engine.impl.persistence.entity.TableDataManager;

public class CommandContextUtil {

    public static FormEngineConfiguration getFormEngineConfiguration() {
        return getFormEngineConfiguration(getCommandContext());
    }
    
    public static FormEngineConfiguration getFormEngineConfiguration(CommandContext commandContext) {
        if (commandContext != null) {
            return (FormEngineConfiguration) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_FORM_ENGINE_CONFIG);
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
        return getFormEngineConfiguration(commandContext).getTableDataManager();
    }
    
    public static FormResourceEntityManager getResourceEntityManager() {
        return getResourceEntityManager(getCommandContext());
    }
    
    public static FormResourceEntityManager getResourceEntityManager(CommandContext commandContext) {
        return getFormEngineConfiguration(commandContext).getResourceEntityManager();
    }
    
    public static FormDeploymentEntityManager getDeploymentEntityManager() {
        return getDeploymentEntityManager(getCommandContext());
    }
    
    public static FormDeploymentEntityManager getDeploymentEntityManager(CommandContext commandContext) {
        return getFormEngineConfiguration(commandContext).getDeploymentEntityManager();
    }
    
    public static FormDefinitionEntityManager getFormDefinitionEntityManager() {
        return getFormDefinitionEntityManager(getCommandContext());
    }
    
    public static FormDefinitionEntityManager getFormDefinitionEntityManager(CommandContext commandContext) {
        return getFormEngineConfiguration(commandContext).getFormDefinitionEntityManager();
    }
    
    public static FormInstanceEntityManager getFormInstanceEntityManager() {
        return getFormInstanceEntityManager(getCommandContext());
    }
    
    public static FormInstanceEntityManager getFormInstanceEntityManager(CommandContext commandContext) {
        return getFormEngineConfiguration(commandContext).getFormInstanceEntityManager();
    }
    
    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
