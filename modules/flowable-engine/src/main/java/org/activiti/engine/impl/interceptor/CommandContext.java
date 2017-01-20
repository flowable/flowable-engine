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
package org.activiti.engine.impl.interceptor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.activiti.engine.ActivitiTaskAlreadyClaimedException;
import org.activiti.engine.FlowableEngineAgenda;
import org.activiti.engine.JobNotFoundException;
import org.activiti.engine.common.api.delegate.event.ActivitiEventDispatcher;
import org.activiti.engine.common.impl.interceptor.AbstractCommandContext;
import org.activiti.engine.common.impl.interceptor.BaseCommandContextCloseListener;
import org.activiti.engine.impl.asyncexecutor.JobManager;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.history.HistoryManager;
import org.activiti.engine.impl.jobexecutor.FailedJobCommandFactory;
import org.activiti.engine.impl.persistence.cache.EntityCache;
import org.activiti.engine.impl.persistence.entity.AttachmentEntityManager;
import org.activiti.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.activiti.engine.impl.persistence.entity.CommentEntityManager;
import org.activiti.engine.impl.persistence.entity.DeadLetterJobEntityManager;
import org.activiti.engine.impl.persistence.entity.DeploymentEntityManager;
import org.activiti.engine.impl.persistence.entity.EventLogEntryEntityManager;
import org.activiti.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricDetailEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricTaskInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.activiti.engine.impl.persistence.entity.IdentityLinkEntityManager;
import org.activiti.engine.impl.persistence.entity.JobEntityManager;
import org.activiti.engine.impl.persistence.entity.ModelEntityManager;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.activiti.engine.impl.persistence.entity.PropertyEntityManager;
import org.activiti.engine.impl.persistence.entity.ResourceEntityManager;
import org.activiti.engine.impl.persistence.entity.SuspendedJobEntityManager;
import org.activiti.engine.impl.persistence.entity.TableDataManager;
import org.activiti.engine.impl.persistence.entity.TaskEntityManager;
import org.activiti.engine.impl.persistence.entity.TimerJobEntityManager;
import org.activiti.engine.impl.persistence.entity.VariableInstanceEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Agim Emruli
 * @author Joram Barrez
 */
public class CommandContext extends AbstractCommandContext {

  private static Logger log = LoggerFactory.getLogger(CommandContext.class);

  protected ProcessEngineConfigurationImpl processEngineConfiguration;
  protected FailedJobCommandFactory failedJobCommandFactory;
  
  protected FlowableEngineAgenda agenda;
  protected Map<String, ExecutionEntity> involvedExecutions = new HashMap<String, ExecutionEntity>(1); // The executions involved with the command
  protected LinkedList<Object> resultStack = new LinkedList<Object>(); // needs to be a stack, as JavaDelegates can do api calls again

  public CommandContext(Command<?> command, ProcessEngineConfigurationImpl processEngineConfiguration) {
    super(command);
    this.processEngineConfiguration = processEngineConfiguration;
    this.failedJobCommandFactory = processEngineConfiguration.getFailedJobCommandFactory();
    this.sessionFactories = processEngineConfiguration.getSessionFactories();
    this.agenda = processEngineConfiguration.getAgendaFactory().createAgenda(this);
  }

  protected void logException() {
    if (exception instanceof JobNotFoundException || exception instanceof ActivitiTaskAlreadyClaimedException) {
      // reduce log level, because this may have been caused because of job deletion due to cancelActiviti="true"
      log.info("Error while closing command context", exception);
    } else {
      super.logException();
    }
  }
  
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void addCloseListener(CommandContextCloseListener commandContextCloseListener) {
    if (closeListeners == null) {
      closeListeners = new ArrayList<BaseCommandContextCloseListener<AbstractCommandContext>>(1);
    }
    closeListeners.add((BaseCommandContextCloseListener) commandContextCloseListener);
  }

  public DbSqlSession getDbSqlSession() {
    return getSession(DbSqlSession.class);
  }

  public EntityCache getEntityCache() {
    return getSession(EntityCache.class);
  }

  public DeploymentEntityManager getDeploymentEntityManager() {
    return processEngineConfiguration.getDeploymentEntityManager();
  }

  public ResourceEntityManager getResourceEntityManager() {
    return processEngineConfiguration.getResourceEntityManager();
  }

  public ByteArrayEntityManager getByteArrayEntityManager() {
    return processEngineConfiguration.getByteArrayEntityManager();
  }

  public ProcessDefinitionEntityManager getProcessDefinitionEntityManager() {
    return processEngineConfiguration.getProcessDefinitionEntityManager();
  }

  public ModelEntityManager getModelEntityManager() {
    return processEngineConfiguration.getModelEntityManager();
  }
  
  public ProcessDefinitionInfoEntityManager getProcessDefinitionInfoEntityManager() {
    return processEngineConfiguration.getProcessDefinitionInfoEntityManager();
  }

  public ExecutionEntityManager getExecutionEntityManager() {
    return processEngineConfiguration.getExecutionEntityManager();
  }

  public TaskEntityManager getTaskEntityManager() {
    return processEngineConfiguration.getTaskEntityManager();
  }

  public IdentityLinkEntityManager getIdentityLinkEntityManager() {
    return processEngineConfiguration.getIdentityLinkEntityManager();
  }

  public VariableInstanceEntityManager getVariableInstanceEntityManager() {
    return processEngineConfiguration.getVariableInstanceEntityManager();
  }

  public HistoricProcessInstanceEntityManager getHistoricProcessInstanceEntityManager() {
    return processEngineConfiguration.getHistoricProcessInstanceEntityManager();
  }

  public HistoricDetailEntityManager getHistoricDetailEntityManager() {
    return processEngineConfiguration.getHistoricDetailEntityManager();
  }

  public HistoricVariableInstanceEntityManager getHistoricVariableInstanceEntityManager() {
    return processEngineConfiguration.getHistoricVariableInstanceEntityManager();
  }

  public HistoricActivityInstanceEntityManager getHistoricActivityInstanceEntityManager() {
    return processEngineConfiguration.getHistoricActivityInstanceEntityManager();
  }

  public HistoricTaskInstanceEntityManager getHistoricTaskInstanceEntityManager() {
    return processEngineConfiguration.getHistoricTaskInstanceEntityManager();
  }

  public HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager() {
    return processEngineConfiguration.getHistoricIdentityLinkEntityManager();
  }

  public EventLogEntryEntityManager getEventLogEntryEntityManager() {
    return processEngineConfiguration.getEventLogEntryEntityManager();
  }

  public JobEntityManager getJobEntityManager() {
    return processEngineConfiguration.getJobEntityManager();
  }
  
  public TimerJobEntityManager getTimerJobEntityManager() {
    return processEngineConfiguration.getTimerJobEntityManager();
  }
  
  public SuspendedJobEntityManager getSuspendedJobEntityManager() {
    return processEngineConfiguration.getSuspendedJobEntityManager();
  }
  
  public DeadLetterJobEntityManager getDeadLetterJobEntityManager() {
    return processEngineConfiguration.getDeadLetterJobEntityManager();
  }

  public AttachmentEntityManager getAttachmentEntityManager() {
    return processEngineConfiguration.getAttachmentEntityManager();
  }

  public TableDataManager getTableDataManager() {
    return processEngineConfiguration.getTableDataManager();
  }

  public CommentEntityManager getCommentEntityManager() {
    return processEngineConfiguration.getCommentEntityManager();
  }

  public PropertyEntityManager getPropertyEntityManager() {
    return processEngineConfiguration.getPropertyEntityManager();
  }

  public EventSubscriptionEntityManager getEventSubscriptionEntityManager() {
    return processEngineConfiguration.getEventSubscriptionEntityManager();
  }

  public HistoryManager getHistoryManager() {
    return processEngineConfiguration.getHistoryManager();
  }
  
  public JobManager getJobManager() {
    return processEngineConfiguration.getJobManager();
  }

  // Involved executions ////////////////////////////////////////////////////////

  public void addInvolvedExecution(ExecutionEntity executionEntity) {
    if (executionEntity.getId() != null) {
      involvedExecutions.put(executionEntity.getId(), executionEntity);
    }
  }

  public boolean hasInvolvedExecutions() {
    return involvedExecutions.size() > 0;
  }

  public Collection<ExecutionEntity> getInvolvedExecutions() {
    return involvedExecutions.values();
  }

  // getters and setters
  // //////////////////////////////////////////////////////

  public FailedJobCommandFactory getFailedJobCommandFactory() {
    return failedJobCommandFactory;
  }

  public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
    return processEngineConfiguration;
  }

  public ActivitiEventDispatcher getEventDispatcher() {
    return processEngineConfiguration.getEventDispatcher();
  }

  public FlowableEngineAgenda getAgenda() {
    return agenda;
  }

  public Object getResult() {
    return resultStack.pollLast();
  }

  public void setResult(Object result) {
    resultStack.add(result);
  }
}
