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

import java.util.HashSet;
import java.util.Set;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.CmmnRepositoryService;
import org.flowable.cmmn.engine.CmmnRuntimeService;
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
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.TableDataManager;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.engine.common.impl.context.Context;
import org.flowable.engine.common.impl.db.DbSqlSession;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.common.impl.interceptor.CommandContext;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.common.impl.persistence.cache.EntityCache;
import org.flowable.variable.service.HistoricVariableService;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.VariableServiceConfiguration;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class CommandContextUtil {
    
    public static final String ATTRIBUTE_INVOLVED_CASE_INSTANCE_IDS = "ctx.attribute.involvedCaseInstanceIds";
    
    public static CmmnEngineConfiguration getCmmnEngineConfiguration() {
        return getCmmnEngineConfiguration(getCommandContext());
    }
    
    public static CmmnEngineConfiguration getCmmnEngineConfiguration(CommandContext commandContext) {
        return (CmmnEngineConfiguration) commandContext.getEngineConfigurations().get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
    }
    
    public static CmmnRepositoryService getCmmnRepositoryService() {
        return getCmmnEngineConfiguration().getCmmnRepositoryService();
    }
    
    public static CmmnRuntimeService getCmmnRuntimeService() {
        return getCmmnEngineConfiguration().getCmmnRuntimeService();
    }
    
    public static ExpressionManager getExpressionManager() {
        return getExpressionManager(getCommandContext());
    }
    
    public static ExpressionManager getExpressionManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getExpressionManager();
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
    
    public static SentryPartInstanceEntityManager getSentryPartInstanceEntityManager() {
        return getSentryPartInstanceEntityManager(getCommandContext());
    }
    
    public static SentryPartInstanceEntityManager getSentryPartInstanceEntityManager(CommandContext commandContext) {
        return getCmmnEngineConfiguration(commandContext).getSentryPartInstanceEntityManager();
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
    
    public static HistoricVariableService getHistoricVariableService() {
        return getHistoricVariableService(getCommandContext());
    }
    
    public static HistoricVariableService getHistoricVariableService(CommandContext commandContext) {
        HistoricVariableService historicVariableService = null;
        VariableServiceConfiguration variableServiceConfiguration = getVariableServiceConfiguration(commandContext);
        if (variableServiceConfiguration != null) {
            historicVariableService = variableServiceConfiguration.getHistoricVariableService();
        }
        return historicVariableService;
    }
    
    public static VariableServiceConfiguration getVariableServiceConfiguration() {
        return getVariableServiceConfiguration(getCommandContext());
    }
    
    public static VariableServiceConfiguration getVariableServiceConfiguration(CommandContext commandContext) {
        return (VariableServiceConfiguration) commandContext.getServiceConfigurations().get(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG);
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
    
    @SuppressWarnings("unchecked")
    public static void addInvolvedCaseInstanceId(CommandContext commandContext, String caseInstanceId) {
        if (caseInstanceId != null) {
            Set<String> involvedCaseInstanceIds = null;
            Object obj = commandContext.getAttribute(ATTRIBUTE_INVOLVED_CASE_INSTANCE_IDS);
            if (obj != null) {
                involvedCaseInstanceIds = (Set<String>) obj;
            } else {
                involvedCaseInstanceIds = new HashSet<>(1); // typically will be only 1 entry
                commandContext.addAttribute(ATTRIBUTE_INVOLVED_CASE_INSTANCE_IDS, involvedCaseInstanceIds);
            }
            involvedCaseInstanceIds.add(caseInstanceId);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static Set<String> getInvolvedCaseInstanceIds(CommandContext commandContext) {
         Object obj = commandContext.getAttribute(ATTRIBUTE_INVOLVED_CASE_INSTANCE_IDS);
         if (obj != null) {
             return (Set<String>) obj;
         }
         return null;
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
