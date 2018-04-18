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
import org.flowable.cmmn.api.history.HistoricVariableInstanceQuery;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.DeleteHistoricCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.DeleteHistoricTaskInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricIdentityLinksForCaseInstanceCmd;
import org.flowable.cmmn.engine.impl.cmd.GetHistoricIdentityLinksForTaskCmd;
import org.flowable.cmmn.engine.impl.history.CmmnHistoricVariableInstanceQueryImpl;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.flowable.task.service.impl.HistoricTaskInstanceQueryImpl;

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
    
}
