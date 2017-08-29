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
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryOnPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.TableDataManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.db.DbSqlSession;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.common.impl.persistence.cache.EntityCache;

/**
 * @author Joram Barrez
 */
public class CommandContextUtil {
    
    public static CmmnEngineConfiguration getCmmnEngineConfiguration() {
        return getCmmnEngineConfiguration(getCommandContext());
    }
    
    public static CmmnEngineConfiguration getCmmnEngineConfiguration(CommandContext commandContext) {
        return (CmmnEngineConfiguration) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
    }
    
    public static CmmnHistoryManager getCmmnHistoryManager() {
        return getCmmnHistoryManager(getCommandContext());
    }
    
    public static CmmnHistoryManager getCmmnHistoryManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getCmmnHistoryManager();
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
    
    public static CaseInstanceEntityManager getCaseInstanceEntityManager() {
        return getCaseInstanceEntityManager(getCommandContext());
    }
    
    public static CaseInstanceEntityManager getCaseInstanceEntityManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getCaseInstanceEntityManager();
    }
    
    public static PlanItemInstanceEntityManager getPlanItemInstanceEntityManager() {
        return getPlanItemInstanceEntityManager(getCommandContext());
    }
    
    public static PlanItemInstanceEntityManager getPlanItemInstanceEntityManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getPlanItemInstanceEntityManager();
    }
    
    public static SentryOnPartInstanceEntityManager getSentryOnPartInstanceEntityManager() {
        return getSentryOnPartInstanceEntityManager(getCommandContext());
    }
    
    public static SentryOnPartInstanceEntityManager getSentryOnPartInstanceEntityManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getSentryOnPartInstanceEntityManager();
    }
    
    public static MilestoneInstanceEntityManager getMilestoneInstanceEntityManager() {
        return getMilestoneInstanceEntityManager(getCommandContext());
    }
    
    public static MilestoneInstanceEntityManager getMilestoneInstanceEntityManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getMilestoneInstanceEntityManager();
    }
    
    public static HistoricCaseInstanceEntityManager getHistoricCaseInstanceEntityManager() {
        return getHistoricCaseInstanceEntityManager(getCommandContext());
    }
    
    public static HistoricCaseInstanceEntityManager getHistoricCaseInstanceEntityManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getHistoricCaseInstanceEntityManager();
    }
    
    public static HistoricMilestoneInstanceEntityManager getHistoricMilestoneInstanceEntityManager() {
        return getHistoricMilestoneInstanceEntityManager(getCommandContext());
    }
    
    public static HistoricMilestoneInstanceEntityManager getHistoricMilestoneInstanceEntityManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getHistoricMilestoneInstanceEntityManager();
    }
    
    public static TableDataManager getTableDataManager() {
        return getTableDataManager(getCommandContext());
    }
    
    public static TableDataManager getTableDataManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getTableDataManager();
    }
    
    public static CmmnEngineAgenda getAgenda() {
        return getAgenda(getCommandContext());
    }
    
    public static CmmnEngineAgenda getAgenda(CommandContext commandContext) {
        return commandContext.getSession(CmmnEngineAgenda.class);
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
    
    public static CaseInstanceHelper getCaseInstanceHelper() {
        return getCaseInstanceHelper(getCommandContext());
    }
    
    public static CaseInstanceHelper getCaseInstanceHelper(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getCaseInstanceHelper();
    }
    
    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
