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

package org.flowable.cmmn.engine.impl;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManager;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityManager;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.flowable.task.service.impl.persistence.entity.TaskEntityManager;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityManager;

/**
 * @author Joram Barrez
 */
public abstract class AbstractCmmnManager {

    protected CmmnEngineConfiguration cmmnEngineConfiguration;

    public AbstractCmmnManager(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    protected CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

    protected <T> T getSession(Class<T> sessionClass) {
        return getCommandContext().getSession(sessionClass);
    }
    
    protected DbSqlSession getDbSqlSession() {
        return getSession(DbSqlSession.class);
    }
    
    protected CmmnResourceEntityManager getCmmnResourceEntityManager() {
        return cmmnEngineConfiguration.getCmmnResourceEntityManager();
    }
    
    protected CmmnDeploymentEntityManager getCmmnDeploymentEntityManager() {
        return cmmnEngineConfiguration.getCmmnDeploymentEntityManager();
    }
    
    protected CaseDefinitionEntityManager getCaseDefinitionEntityManager() {
        return cmmnEngineConfiguration.getCaseDefinitionEntityManager();
    }
    
    protected CaseInstanceEntityManager getCaseInstanceEntityManager() {
        return cmmnEngineConfiguration.getCaseInstanceEntityManager();
    }
    
    protected PlanItemInstanceEntityManager getPlanItemInstanceEntityManager() {
        return cmmnEngineConfiguration.getPlanItemInstanceEntityManager();
    }
    
    protected SentryPartInstanceEntityManager getSentryPartInstanceEntityManager() {
        return cmmnEngineConfiguration.getSentryPartInstanceEntityManager();
    }
    
    protected MilestoneInstanceEntityManager getMilestoneInstanceEntityManager() {
        return cmmnEngineConfiguration.getMilestoneInstanceEntityManager();
    }
    
    protected HistoricCaseInstanceEntityManager getHistoricCaseInstanceEntityManager() {
        return cmmnEngineConfiguration.getHistoricCaseInstanceEntityManager();
    }
    
    protected HistoricMilestoneInstanceEntityManager getHistoricMilestoneInstanceEntityManager() {
        return cmmnEngineConfiguration.getHistoricMilestoneInstanceEntityManager();
    }
    
    protected VariableInstanceEntityManager getVariableInstanceEntityManager() {
        return cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableInstanceEntityManager();
    }
    
    protected HistoricVariableInstanceEntityManager getHistoricVariableInstanceEntityManager() {
        return cmmnEngineConfiguration.getVariableServiceConfiguration().getHistoricVariableInstanceEntityManager();
    }
    
    protected IdentityLinkEntityManager getIdentityLinkEntityManager() {
        return cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
    }
    
    protected HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager() {
        return cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkEntityManager();
    }
    
    protected TaskEntityManager getTaskEntityManager() {
        return cmmnEngineConfiguration.getTaskServiceConfiguration().getTaskEntityManager();
    }
    
    protected HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager() {
        return cmmnEngineConfiguration.getTaskServiceConfiguration().getHistoricTaskInstanceEntityManager();
    }

    protected CmmnEngineConfiguration getCmmnEngineConfiguration() {
        return cmmnEngineConfiguration;
    }

}
