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

package org.flowable.engine.impl.cfg;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.Event;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.ExternalWorkerServiceTask;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOParameter;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.persistence.CountingExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.BpmnLoggingSessionUtil;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.CountingEntityUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.Job;
import org.flowable.job.service.ScopeAwareInternalJobManager;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.JobInfoEntity;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * @author Tijs Rademakers
 */
public class DefaultInternalJobManager extends ScopeAwareInternalJobManager {
    
    protected ProcessEngineConfigurationImpl processEngineConfiguration;

    public DefaultInternalJobManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }
    
    @Override
    protected VariableScope resolveVariableScopeInternal(Job job) {
        if (job.getExecutionId() != null) {
            return getExecutionEntityManager().findById(job.getExecutionId());
        }
        return null;
    }

    @Override
    public Map<String, Object> resolveVariablesForExternalWorkerJobInternal(ExternalWorkerJob job) {
        String executionId = job.getExecutionId();
        if (executionId != null) {
            ExecutionEntity executionEntity = getExecutionEntityManager().findById(executionId);
            if (executionEntity == null) {
                return null;
            }
            FlowElement currentFlowElement = executionEntity.getCurrentFlowElement();
            if (currentFlowElement instanceof ExternalWorkerServiceTask externalWorkerServiceTask) {
                List<IOParameter> inParameters = externalWorkerServiceTask.getInParameters();
                if (inParameters != null && !inParameters.isEmpty()) {
                    Map<String, Object> variables = new HashMap<>();
                    for (IOParameter inParameter : inParameters) {
                        if (inParameter.getSource() != null) {
                            variables.put(inParameter.getTarget(), executionEntity.getVariable(inParameter.getSource()));
                        } else {
                            Expression sourceExpression = processEngineConfiguration.getExpressionManager()
                                    .createExpression(inParameter.getSourceExpression());
                            Object value = sourceExpression.getValue(executionEntity);
                            variables.put(inParameter.getTarget(), value);
                        }
                    }
                    return variables;
                } else if (externalWorkerServiceTask.isDoNotIncludeVariables()) {
                    return Collections.emptyMap();
                }
            }
            return executionEntity.getVariables();
        }
        return null;
    }

    @Override
    protected boolean handleJobInsertInternal(Job job) {
        // add link to execution
        if (job.getExecutionId() != null) {
            ExecutionEntity execution = getExecutionEntityManager().findById(job.getExecutionId());
            if (execution != null) {
                
                // Inherit tenant if (if applicable)
                if (execution.getTenantId() != null) {
                    ((AbstractRuntimeJobEntity) job).setTenantId(execution.getTenantId());
                }
                
                CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) execution;
                
                if (job instanceof TimerJobEntity timerJobEntity) {
                    if (CountingEntityUtil.isExecutionRelatedEntityCountEnabled(execution)) {
                        countingExecutionEntity.setTimerJobCount(countingExecutionEntity.getTimerJobCount() + 1);
                    }
                    
                } else if (job instanceof JobEntity jobEntity) {

                    if (CountingEntityUtil.isExecutionRelatedEntityCountEnabled(execution)) {
                        countingExecutionEntity.setJobCount(countingExecutionEntity.getJobCount() + 1);
                    }
                
                } else {
                    if (CountingEntityUtil.isExecutionRelatedEntityCountEnabled(execution)) {
                        if (job instanceof SuspendedJobEntity) {
                            countingExecutionEntity.setSuspendedJobCount(countingExecutionEntity.getSuspendedJobCount() + 1);
                        } else if (job instanceof DeadLetterJobEntity) {
                            countingExecutionEntity.setDeadLetterJobCount(countingExecutionEntity.getDeadLetterJobCount() + 1);
                        } else if (job instanceof ExternalWorkerJobEntity) {
                            countingExecutionEntity.setExternalWorkerJobCount(countingExecutionEntity.getExternalWorkerJobCount() + 1);
                        }
                    }
                }

            } else {
                // In case the job has an executionId, but the Execution was not found,
                // it means that for example for a boundary timer event on a user task,
                // the task has been completed and the Execution and job have been removed.
                return false;
            }
        }
        
        return true;
    }

    @Override
    protected void handleJobDeleteInternal(Job job) {
        if (job.getExecutionId() != null && CountingEntityUtil.isExecutionRelatedEntityCountEnabledGlobally()) {
            ExecutionEntity executionEntity = getExecutionEntityManager().findById(job.getExecutionId());
            if (CountingEntityUtil.isExecutionRelatedEntityCountEnabled(executionEntity)) {
                CountingExecutionEntity countingExecutionEntity = (CountingExecutionEntity) executionEntity;
                if (job instanceof JobEntity) {
                    countingExecutionEntity.setJobCount(countingExecutionEntity.getJobCount() - 1);
                
                } else if (job instanceof TimerJobEntity) {
                    countingExecutionEntity.setTimerJobCount(countingExecutionEntity.getTimerJobCount() - 1);
                
                } else if (job instanceof SuspendedJobEntity) {
                    countingExecutionEntity.setSuspendedJobCount(countingExecutionEntity.getSuspendedJobCount() - 1);
                
                } else if (job instanceof DeadLetterJobEntity) {
                    countingExecutionEntity.setDeadLetterJobCount(countingExecutionEntity.getDeadLetterJobCount() - 1);
                } else if (job instanceof ExternalWorkerJobEntity) {
                    countingExecutionEntity.setExternalWorkerJobCount(countingExecutionEntity.getExternalWorkerJobCount() - 1);
                }
            }
        }
    }

    @Override
    protected void lockJobScopeInternal(Job job) {
        ExecutionEntityManager executionEntityManager = getExecutionEntityManager();
        ExecutionEntity execution = executionEntityManager.findById(job.getExecutionId());
        if (execution != null) {
            String lockOwner = null;
            Date lockExpirationTime = null;

            if (job instanceof JobInfoEntity) {
                lockOwner = ((JobInfoEntity) job).getLockOwner();
                lockExpirationTime = ((JobInfoEntity) job).getLockExpirationTime();
            }

            if (lockOwner == null || lockExpirationTime == null) {
                int lockMillis = processEngineConfiguration.getAsyncExecutor().getAsyncJobLockTimeInMillis();
                GregorianCalendar lockCal = new GregorianCalendar();
                lockCal.setTime(processEngineConfiguration.getClock().getCurrentTime());
                lockCal.add(Calendar.MILLISECOND, lockMillis);

                lockOwner = processEngineConfiguration.getAsyncExecutor().getLockOwner();
                lockExpirationTime = lockCal.getTime();
            }

            executionEntityManager.updateProcessInstanceLockTime(execution.getProcessInstanceId(), lockOwner, lockExpirationTime);

            if (processEngineConfiguration.isLoggingSessionEnabled()) {
                FlowElement flowElement = execution.getCurrentFlowElement();
                BpmnLoggingSessionUtil.addAsyncActivityLoggingData("Locking job for " + flowElement.getId() + ", with job id " + job.getId(),
                        LoggingSessionConstants.TYPE_SERVICE_TASK_LOCK_JOB, (JobEntity) job, flowElement, execution);
            }
        }

    }

    @Override
    protected void clearJobScopeLockInternal(Job job) {
        ExecutionEntityManager executionEntityManager = getExecutionEntityManager();
        ExecutionEntity execution = executionEntityManager.findById(job.getProcessInstanceId());
        if (execution != null) {
            executionEntityManager.clearProcessInstanceLockTime(execution.getId());
        }

        if (processEngineConfiguration.isLoggingSessionEnabled()) {
            ExecutionEntity localExecution = executionEntityManager.findById(job.getExecutionId());
            FlowElement flowElement = localExecution.getCurrentFlowElement();
            BpmnLoggingSessionUtil.addAsyncActivityLoggingData("Unlocking job for " + flowElement.getId() + ", with job id " + job.getId(),
                            LoggingSessionConstants.TYPE_SERVICE_TASK_UNLOCK_JOB, (JobEntity) job, flowElement, localExecution);
        }
    }

    @Override
    protected void preTimerJobDeleteInternal(JobEntity jobEntity, VariableScope variableScope) {
        String activityId = jobEntity.getJobHandlerConfiguration();

        if (jobEntity.getJobHandlerType().equalsIgnoreCase(TimerStartEventJobHandler.TYPE) ||
                        jobEntity.getJobHandlerType().equalsIgnoreCase(TriggerTimerEventJobHandler.TYPE)) {

            activityId = TimerEventHandler.getActivityIdFromConfiguration(jobEntity.getJobHandlerConfiguration());
            String endDateExpressionString = TimerEventHandler.getEndDateFromConfiguration(jobEntity.getJobHandlerConfiguration());

            if (endDateExpressionString != null) {
                Expression endDateExpression = processEngineConfiguration.getExpressionManager().createExpression(endDateExpressionString);

                String endDateString = null;

                BusinessCalendar businessCalendar = processEngineConfiguration.getBusinessCalendarManager().getBusinessCalendar(
                        getBusinessCalendarName(TimerEventHandler.getCalendarNameFromConfiguration(jobEntity.getJobHandlerConfiguration()), variableScope));

                if (endDateExpression != null) {
                    Object endDateValue = endDateExpression.getValue(variableScope);
                    if (endDateValue instanceof String) {
                        endDateString = (String) endDateValue;
                    } else if (endDateValue instanceof Date) {
                        jobEntity.setEndDate((Date) endDateValue);
                    } else {
                        throw new FlowableException("Timer '" + ((ExecutionEntity) variableScope).getActivityId()
                                + "' in " + variableScope + " was not configured with a valid duration/time, either hand in a java.util.Date or a String in format 'yyyy-MM-dd'T'hh:mm:ss'");
                    }

                    if (jobEntity.getEndDate() == null) {
                        jobEntity.setEndDate(businessCalendar.resolveEndDate(endDateString));
                    }
                }
            }
        }

        int maxIterations = 1;
        if (jobEntity.getProcessDefinitionId() != null) {
            org.flowable.bpmn.model.Process process = ProcessDefinitionUtil.getProcess(jobEntity.getProcessDefinitionId());
            maxIterations = getMaxIterations(process, activityId);
            if (maxIterations <= 1) {
                maxIterations = getMaxIterations(process, activityId);
            }
        }
        jobEntity.setMaxIterations(maxIterations);
    }
    
    @Override
    protected void preRepeatedTimerScheduleInternal(TimerJobEntity ti, VariableScope variableScope) {
        // Nothing to do
    }

    protected int getMaxIterations(org.flowable.bpmn.model.Process process, String activityId) {
        FlowElement flowElement = process.getFlowElement(activityId, true);
        if (flowElement != null) {
            if (flowElement instanceof Event event) {

                List<EventDefinition> eventDefinitions = event.getEventDefinitions();

                if (eventDefinitions != null) {

                    for (EventDefinition eventDefinition : eventDefinitions) {
                        if (eventDefinition instanceof TimerEventDefinition timerEventDefinition) {
                            if (timerEventDefinition.getTimeCycle() != null) {
                                return calculateMaxIterationsValue(timerEventDefinition.getTimeCycle());
                            }
                        }
                    }

                }

            }
        }
        return -1;
    }
    
    protected int calculateMaxIterationsValue(String originalExpression) {
        int times = Integer.MAX_VALUE;
        List<String> expression = Arrays.asList(originalExpression.split("/"));
        if (expression.size() > 1 && expression.get(0).startsWith("R")) {
            if (expression.get(0).length() > 1) {
                times = Integer.parseInt(expression.get(0).substring(1));
            }
        }
        
        return times;
    }
    
    protected String getBusinessCalendarName(String calendarName, VariableScope variableScope) {
        String businessCalendarName = CycleBusinessCalendar.NAME;
        if (StringUtils.isNotEmpty(calendarName)) {
            businessCalendarName = (String) CommandContextUtil.getProcessEngineConfiguration().getExpressionManager()
                    .createExpression(calendarName).getValue(variableScope);
        }
        return businessCalendarName;
    }

    protected ExecutionEntityManager getExecutionEntityManager() {
        return processEngineConfiguration.getExecutionEntityManager();
    }

}
