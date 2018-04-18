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
package org.flowable.cmmn.engine.impl.history;

import java.util.Date;
import java.util.List;

import org.flowable.cmmn.api.history.HistoricMilestoneInstance;
import org.flowable.cmmn.api.history.HistoricMilestoneInstanceQuery;
import org.flowable.cmmn.engine.impl.runtime.MilestoneInstanceQueryProperty;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.AbstractQuery;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

/**
 * @author Joram Barrez
 */
public class HistoricMilestoneInstanceQueryImpl extends AbstractQuery<HistoricMilestoneInstanceQuery, HistoricMilestoneInstance> implements HistoricMilestoneInstanceQuery {
    
    protected String name;
    protected String caseInstanceId;
    protected String caseDefinitionId;
    protected Date reachedBefore;
    protected Date reachedAfter;
    
    public HistoricMilestoneInstanceQueryImpl() {
        
    }
    
    public HistoricMilestoneInstanceQueryImpl(CommandExecutor commandExecutor) {
        super(commandExecutor);
    }

    @Override
    public HistoricMilestoneInstanceQuery milestoneInstanceName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public HistoricMilestoneInstanceQuery milestoneInstanceCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
        return this;
    }

    @Override
    public HistoricMilestoneInstanceQuery milestoneInstanceCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
        return this;
    }

    @Override
    public HistoricMilestoneInstanceQuery milestoneInstanceReachedBefore(Date reachedBefore) {
        this.reachedBefore = reachedBefore;
        return this;
    }

    @Override
    public HistoricMilestoneInstanceQuery milestoneInstanceReachedAfter(Date reachedAfter) {
        this.reachedAfter = reachedAfter;
        return this;
    }

    @Override
    public HistoricMilestoneInstanceQuery orderByMilestoneName() {
        return orderBy(MilestoneInstanceQueryProperty.MILESTONE_NAME);
    }

    @Override
    public HistoricMilestoneInstanceQuery orderByTimeStamp() {
        return orderBy(MilestoneInstanceQueryProperty.MILESTONE_TIMESTAMP);
    }
    
    @Override
    public long executeCount(CommandContext commandContext) {
        return CommandContextUtil.getHistoricMilestoneInstanceEntityManager(commandContext).findHistoricMilestoneInstanceCountByQueryCriteria(this);
    }

    @Override
    public List<HistoricMilestoneInstance> executeList(CommandContext commandContext) {
        return CommandContextUtil.getHistoricMilestoneInstanceEntityManager(commandContext).findHistoricMilestoneInstancesByQueryCriteria(this);
    }

    public String getName() {
        return name;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public Date getReachedBefore() {
        return reachedBefore;
    }

    public Date getReachedAfter() {
        return reachedAfter;
    }
    
}
