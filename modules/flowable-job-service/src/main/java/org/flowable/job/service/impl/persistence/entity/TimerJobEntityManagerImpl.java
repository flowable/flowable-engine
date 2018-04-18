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

package org.flowable.job.service.impl.persistence.entity;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.Page;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.TimerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.data.TimerJobDataManager;
import org.flowable.variable.api.delegate.VariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class TimerJobEntityManagerImpl extends AbstractEntityManager<TimerJobEntity> implements TimerJobEntityManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimerJobEntityManagerImpl.class);

    protected TimerJobDataManager jobDataManager;

    public TimerJobEntityManagerImpl(JobServiceConfiguration jobServiceConfiguration, TimerJobDataManager jobDataManager) {
        super(jobServiceConfiguration);
        this.jobDataManager = jobDataManager;
    }

    @Override
    public TimerJobEntity createAndCalculateNextTimer(JobEntity timerEntity, VariableScope variableScope) {
        int repeatValue = calculateRepeatValue(timerEntity);
        if (repeatValue != 0) {
            if (repeatValue > 0) {
                setNewRepeat(timerEntity, repeatValue);
            }
            Date newTimer = calculateNextTimer(timerEntity, variableScope);
            if (newTimer != null && isValidTime(timerEntity, newTimer, variableScope)) {
                TimerJobEntity te = createTimer(timerEntity);
                te.setDuedate(newTimer);
                return te;
            }
        }
        return null;
    }

    @Override
    public List<TimerJobEntity> findTimerJobsToExecute(Page page) {
        return jobDataManager.findTimerJobsToExecute(page);
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionId(String jobHandlerType, String processDefinitionId) {
        return jobDataManager.findJobsByTypeAndProcessDefinitionId(jobHandlerType, processDefinitionId);
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyNoTenantId(String jobHandlerType, String processDefinitionKey) {
        return jobDataManager.findJobsByTypeAndProcessDefinitionKeyNoTenantId(jobHandlerType, processDefinitionKey);
    }

    @Override
    public List<TimerJobEntity> findJobsByTypeAndProcessDefinitionKeyAndTenantId(String jobHandlerType, String processDefinitionKey, String tenantId) {
        return jobDataManager.findJobsByTypeAndProcessDefinitionKeyAndTenantId(jobHandlerType, processDefinitionKey, tenantId);
    }

    @Override
    public List<TimerJobEntity> findJobsByExecutionId(String id) {
        return jobDataManager.findJobsByExecutionId(id);
    }

    @Override
    public List<TimerJobEntity> findJobsByProcessInstanceId(String id) {
        return jobDataManager.findJobsByProcessInstanceId(id);
    }

    @Override
    public List<Job> findJobsByQueryCriteria(TimerJobQueryImpl jobQuery) {
        return jobDataManager.findJobsByQueryCriteria(jobQuery);
    }

    @Override
    public long findJobCountByQueryCriteria(TimerJobQueryImpl jobQuery) {
        return jobDataManager.findJobCountByQueryCriteria(jobQuery);
    }

    @Override
    public void updateJobTenantIdForDeployment(String deploymentId, String newTenantId) {
        jobDataManager.updateJobTenantIdForDeployment(deploymentId, newTenantId);
    }

    @Override
    public boolean insertTimerJobEntity(TimerJobEntity timerJobEntity) {
        return doInsert(timerJobEntity, true);
    }

    @Override
    public void insert(TimerJobEntity jobEntity) {
        insert(jobEntity, true);
    }

    @Override
    public void insert(TimerJobEntity jobEntity, boolean fireCreateEvent) {
        doInsert(jobEntity, fireCreateEvent);
    }

    protected boolean doInsert(TimerJobEntity jobEntity, boolean fireCreateEvent) {
        boolean handledJob = getJobServiceConfiguration().getInternalJobManager().handleJobInsert(jobEntity);
        if (!handledJob) {
            return false;
        }

        jobEntity.setCreateTime(getJobServiceConfiguration().getClock().getCurrentTime());
        super.insert(jobEntity, fireCreateEvent);
        return true;
    }

    @Override
    public void delete(TimerJobEntity jobEntity) {
        super.delete(jobEntity);

        deleteByteArrayRef(jobEntity.getExceptionByteArrayRef());
        deleteByteArrayRef(jobEntity.getCustomValuesByteArrayRef());

        getJobServiceConfiguration().getInternalJobManager().handleJobDelete(jobEntity);

        // Send event
        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, this));
        }
    }
    
    protected TimerJobEntity createTimer(JobEntity te) {
        TimerJobEntity newTimerEntity = create();
        newTimerEntity.setJobHandlerConfiguration(te.getJobHandlerConfiguration());
        newTimerEntity.setCustomValues(te.getCustomValues());
        newTimerEntity.setJobHandlerType(te.getJobHandlerType());
        newTimerEntity.setExclusive(te.isExclusive());
        newTimerEntity.setRepeat(te.getRepeat());
        newTimerEntity.setRetries(te.getRetries());
        newTimerEntity.setEndDate(te.getEndDate());
        newTimerEntity.setExecutionId(te.getExecutionId());
        newTimerEntity.setProcessInstanceId(te.getProcessInstanceId());
        newTimerEntity.setProcessDefinitionId(te.getProcessDefinitionId());
        newTimerEntity.setScopeId(te.getScopeId());
        newTimerEntity.setSubScopeId(te.getSubScopeId());
        newTimerEntity.setScopeDefinitionId(te.getScopeDefinitionId());
        newTimerEntity.setScopeType(te.getScopeType());

        // Inherit tenant
        newTimerEntity.setTenantId(te.getTenantId());
        newTimerEntity.setJobType(JobEntity.JOB_TYPE_TIMER);
        return newTimerEntity;
    }

    protected void setNewRepeat(JobEntity timerEntity, int newRepeatValue) {
        List<String> expression = Arrays.asList(timerEntity.getRepeat().split("/"));
        expression = expression.subList(1, expression.size());
        StringBuilder repeatBuilder = new StringBuilder("R");
        repeatBuilder.append(newRepeatValue);
        for (String value : expression) {
            repeatBuilder.append("/");
            repeatBuilder.append(value);
        }
        timerEntity.setRepeat(repeatBuilder.toString());
    }

    protected boolean isValidTime(JobEntity timerEntity, Date newTimerDate, VariableScope variableScope) {
        BusinessCalendar businessCalendar = getJobServiceConfiguration().getBusinessCalendarManager().getBusinessCalendar(
                        getJobServiceConfiguration().getJobManager().getBusinessCalendarName(timerEntity, variableScope));
        return businessCalendar.validateDuedate(timerEntity.getRepeat(), timerEntity.getMaxIterations(), timerEntity.getEndDate(), newTimerDate);
    }

    protected Date calculateNextTimer(JobEntity timerEntity, VariableScope variableScope) {
        BusinessCalendar businessCalendar = getJobServiceConfiguration().getBusinessCalendarManager().getBusinessCalendar(
                        getJobServiceConfiguration().getJobManager().getBusinessCalendarName(timerEntity, variableScope));
        return businessCalendar.resolveDuedate(timerEntity.getRepeat(), timerEntity.getMaxIterations());
    }

    protected int calculateRepeatValue(JobEntity timerEntity) {
        int times = -1;
        List<String> expression = Arrays.asList(timerEntity.getRepeat().split("/"));
        if (expression.size() > 1 && expression.get(0).startsWith("R") && expression.get(0).length() > 1) {
            times = Integer.parseInt(expression.get(0).substring(1));
            if (times > 0) {
                times--;
            }
        }
        return times;
    }

    @Override
    protected TimerJobDataManager getDataManager() {
        return jobDataManager;
    }

    public void setJobDataManager(TimerJobDataManager jobDataManager) {
        this.jobDataManager = jobDataManager;
    }
}
