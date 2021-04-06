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

import java.util.List;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.StageResponse;
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.history.HistoricMilestoneInstanceQuery;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.api.history.HistoricVariableInstanceQuery;
import org.flowable.cmmn.api.reactivation.CaseReactivationBuilder;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.CmmnDeleteHistoricTaskLogEntryCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteHistoricCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteHistoricTaskInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricEntityLinkChildrenForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricEntityLinkChildrenWithSameRootAsCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricEntityLinkParentsForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricIdentityLinksForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricIdentityLinksForPlanItemInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricIdentityLinksForTaskCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricStageOverviewCmd;
import org.flowable.cmmn.engine.impl.history.CmmnHistoricVariableInstanceQueryImpl;
import org.flowable.cmmn.engine.impl.reactivation.CaseReactivationBuilderImpl;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder;
import org.flowable.task.api.history.HistoricTaskLogEntryQuery;
import org.flowable.task.api.history.NativeHistoricTaskLogEntryQuery;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.HistoricTaskLogEntryBuilderImpl;
import org.flowable.task.service.impl.HistoricTaskLogEntryQueryImpl;
import org.flowable.task.service.impl.NativeHistoricTaskLogEntryQueryImpl;

/**
 * @author Joram Barrez
 */
public class CmmnHistoryServiceImpl extends CommonEngineServiceImpl<CmmnEngineConfiguration> implements CmmnHistoryService {

    public CmmnHistoryServiceImpl(CmmnEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    @Override
    public HistoricCaseInstanceQuery createHistoricCaseInstanceQuery() {
        return configuration.getHistoricCaseInstanceEntityManager().createHistoricCaseInstanceQuery();
    }

    @Override
    public HistoricMilestoneInstanceQuery createHistoricMilestoneInstanceQuery() {
        return configuration.getHistoricMilestoneInstanceEntityManager().createHistoricMilestoneInstanceQuery();
    }
    
    @Override
    public HistoricVariableInstanceQuery createHistoricVariableInstanceQuery() {
        return new CmmnHistoricVariableInstanceQueryImpl(commandExecutor, configuration);
    }

    @Override
    public HistoricPlanItemInstanceQuery createHistoricPlanItemInstanceQuery() {
        return configuration.getHistoricPlanItemInstanceEntityManager().createHistoricPlanItemInstanceQuery();
    }

    @Override
    public List<StageResponse> getStageOverview(String caseInstanceId) {
        return commandExecutor.execute(new GetHistoricStageOverviewCmd(caseInstanceId));
    }

    @Override
    public void deleteHistoricCaseInstance(String caseInstanceId) {
        commandExecutor.execute(new DeleteHistoricCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public HistoricTaskInstanceQuery createHistoricTaskInstanceQuery() {
        return new HistoricTaskInstanceQueryImpl(commandExecutor, configuration.getDatabaseType(),
                configuration.getTaskServiceConfiguration(), configuration.getVariableServiceConfiguration());
    }
    
    @Override
    public void deleteHistoricTaskInstance(String taskId) {
        commandExecutor.execute(new DeleteHistoricTaskInstanceCmd(taskId));
    }

    @Override
    public CaseReactivationBuilder createCaseReactivationBuilder(String caseInstanceId) {
        return new CaseReactivationBuilderImpl(commandExecutor, caseInstanceId);
    }

    @Override
    public List<HistoricIdentityLink> getHistoricIdentityLinksForCaseInstance(String caseInstanceId) {
        return commandExecutor.execute(new GetHistoricIdentityLinksForCaseInstanceCmd(caseInstanceId));
    }
    
    @Override
    public List<HistoricIdentityLink> getHistoricIdentityLinksForPlanItemInstance(String planItemInstanceId) {
        return commandExecutor.execute(new GetHistoricIdentityLinksForPlanItemInstanceCmd(planItemInstanceId));
    }

    @Override
    public List<HistoricIdentityLink> getHistoricIdentityLinksForTask(String taskId) {
        return commandExecutor.execute(new GetHistoricIdentityLinksForTaskCmd(taskId));
    }

    @Override
    public List<HistoricEntityLink> getHistoricEntityLinkChildrenForCaseInstance(String caseInstanceId) {
        return commandExecutor.execute(new GetHistoricEntityLinkChildrenForCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public List<HistoricEntityLink> getHistoricEntityLinkChildrenWithSameRootAsCaseInstance(String caseInstanceId) {
        return commandExecutor.execute(new GetHistoricEntityLinkChildrenWithSameRootAsCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public List<HistoricEntityLink> getHistoricEntityLinkParentsForCaseInstance(String caseInstanceId) {
        return commandExecutor.execute(new GetHistoricEntityLinkParentsForCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public void deleteHistoricTaskLogEntry(long logNumber) {
        commandExecutor.execute(new CmmnDeleteHistoricTaskLogEntryCmd(logNumber, configuration));
    }

    @Override
    public HistoricTaskLogEntryBuilder createHistoricTaskLogEntryBuilder(TaskInfo task) {
        return new HistoricTaskLogEntryBuilderImpl(commandExecutor, task, configuration.getTaskServiceConfiguration());
    }

    @Override
    public HistoricTaskLogEntryBuilder createHistoricTaskLogEntryBuilder() {
        return new HistoricTaskLogEntryBuilderImpl(commandExecutor, configuration.getTaskServiceConfiguration());
    }

    @Override
    public HistoricTaskLogEntryQuery createHistoricTaskLogEntryQuery() {
        return new HistoricTaskLogEntryQueryImpl(commandExecutor, configuration.getTaskServiceConfiguration());
    }

    @Override
    public NativeHistoricTaskLogEntryQuery createNativeHistoricTaskLogEntryQuery() {
        return new NativeHistoricTaskLogEntryQueryImpl(commandExecutor, configuration.getTaskServiceConfiguration());
    }

}
