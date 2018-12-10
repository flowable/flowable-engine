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
import org.flowable.cmmn.api.history.HistoricCaseInstanceQuery;
import org.flowable.cmmn.api.history.HistoricMilestoneInstanceQuery;
import org.flowable.cmmn.api.history.HistoricPlanItemInstanceQuery;
import org.flowable.cmmn.api.history.HistoricVariableInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.CmmnDeleteTaskLogEntryCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteHistoricCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteHistoricTaskInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricEntityLinkChildrenForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricEntityLinkParentsForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricIdentityLinksForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricIdentityLinksForTaskCmd;
import org.flowable.cmmn.engine.impl.history.CmmnHistoricVariableInstanceQueryImpl;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.NativeTaskLogEntryQuery;
import org.flowable.task.api.TaskInfo;
import org.flowable.task.api.TaskLogEntryBuilder;
import org.flowable.task.api.TaskLogEntryQuery;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.task.service.impl.NativeTaskLogEntryQueryImpl;
import org.flowable.task.service.impl.TaskLogEntryBuilderImpl;
import org.flowable.task.service.impl.TaskLogEntryQueryImpl;

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
        return new CmmnHistoricVariableInstanceQueryImpl(commandExecutor);
    }

    @Override
    public HistoricPlanItemInstanceQuery createHistoricPlanItemInstanceQuery() {
        return configuration.getHistoricPlanItemInstanceEntityManager().createHistoricPlanItemInstanceQuery();
    }

    @Override
    public void deleteHistoricCaseInstance(String caseInstanceId) {
        commandExecutor.execute(new DeleteHistoricCaseInstanceCmd(caseInstanceId));
    }

    @Override
    public HistoricTaskInstanceQuery createHistoricTaskInstanceQuery() {
        return new HistoricTaskInstanceQueryImpl(commandExecutor);
    }
    
    @Override
    public void deleteHistoricTaskInstance(String taskId) {
        commandExecutor.execute(new DeleteHistoricTaskInstanceCmd(taskId));
    }
    
    @Override
    public List<HistoricIdentityLink> getHistoricIdentityLinksForCaseInstance(String caseInstanceId) {
        return commandExecutor.execute(new GetHistoricIdentityLinksForCaseInstanceCmd(caseInstanceId));
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
    public List<HistoricEntityLink> getHistoricEntityLinkParentsForCaseInstance(String caseInstanceId) {
        return commandExecutor.execute(new GetHistoricEntityLinkParentsForCaseInstanceCmd(caseInstanceId));
    }


    @Override
    public void deleteTaskLogEntry(long logNumber) {
        commandExecutor.execute(new CmmnDeleteTaskLogEntryCmd(logNumber));
    }

    @Override
    public TaskLogEntryBuilder createTaskLogEntryBuilder(TaskInfo task) {
        return new TaskLogEntryBuilderImpl(commandExecutor, task);
    }

    @Override
    public TaskLogEntryBuilder createTaskLogEntryBuilder() {
        return new TaskLogEntryBuilderImpl(commandExecutor);
    }

    @Override
    public TaskLogEntryQuery createTaskLogEntryQuery() {
        return new TaskLogEntryQueryImpl(commandExecutor);
    }

    @Override
    public NativeTaskLogEntryQuery createNativeTaskLogEntryQuery() {
        return new NativeTaskLogEntryQueryImpl(commandExecutor);
    }

}
