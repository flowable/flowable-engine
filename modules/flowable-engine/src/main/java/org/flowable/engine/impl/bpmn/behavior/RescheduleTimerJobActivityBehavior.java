package org.flowable.engine.impl.bpmn.behavior;

import java.util.List;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.Expression;
import org.flowable.engine.impl.TimerJobQueryImpl;
import org.flowable.engine.impl.asyncexecutor.JobManager;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.persistence.entity.TimerJobEntity;
import org.flowable.engine.runtime.Job;


public class RescheduleTimerJobActivityBehavior extends AbstractBpmnActivityBehavior {
  private static final long serialVersionUID = 1L;
  
  protected Expression catchingEventId;

  @Override
  public void execute(DelegateExecution execution) {
    String catchingEventIdStr = getStringFromField(catchingEventId, execution);
    
    JobManager jobManager = Context.getProcessEngineConfiguration().getJobManager();
    
    List<Job> jobs = new TimerJobQueryImpl().timers().processInstanceId(execution.getProcessInstanceId()).list();
    for(Job job : jobs) {
      String jobActivityId = TimerEventHandler.getActivityIdFromConfiguration(job.getJobHandlerConfiguration());
      if(jobActivityId.equals(catchingEventIdStr) && Job.JOB_TYPE_TIMER.equals(job.getJobType())) {
          jobManager.rescheduleTimerJob((TimerJobEntity) job);
      }
    }
  }
  
  protected String getStringFromField(Expression expression, DelegateExecution execution) {
    if (expression != null) {
      Object value = expression.getValue(execution);
      if (value != null) {
        return value.toString();
      }
    }
    return null;
  } 
  
  protected boolean inSameScope(DelegateExecution rescheduleExecution, DelegateExecution jobExecution) {
    DelegateExecution rescheduleScopeExecution = findScopeExecution(rescheduleExecution);
    DelegateExecution jobScopeExecution = findScopeExecution(jobExecution);
    return rescheduleScopeExecution.getId().equals(jobScopeExecution.getId());
  }
  
  protected DelegateExecution findScopeExecution(DelegateExecution execution) {
    while(!execution.isScope()) {
      execution = execution.getParent();
    }
    return execution;
  }
}
