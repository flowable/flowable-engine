package org.flowable.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.JobEntity;
import org.flowable.engine.impl.util.Flowable5Util;
import org.flowable.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */

public class DeleteJobCmd implements Command<Object>, Serializable {

  private static final Logger log = LoggerFactory.getLogger(DeleteJobCmd.class);
  private static final long serialVersionUID = 1L;

  protected String jobId;

  public DeleteJobCmd(String jobId) {
    this.jobId = jobId;
  }

  public Object execute(CommandContext commandContext) {
    JobEntity jobToDelete = getJobToDelete(commandContext);
    
    if (Flowable5Util.isFlowable5ProcessDefinitionId(commandContext, jobToDelete.getProcessDefinitionId())) {
      Flowable5CompatibilityHandler activiti5CompatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler(); 
      activiti5CompatibilityHandler.deleteJob(jobToDelete.getId());
      return null;
    }

    sendCancelEvent(jobToDelete);

    commandContext.getJobEntityManager().delete(jobToDelete);
    return null;
  }

  protected void sendCancelEvent(JobEntity jobToDelete) {
    if (Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
      Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, jobToDelete));
    }
  }

  protected JobEntity getJobToDelete(CommandContext commandContext) {
    if (jobId == null) {
      throw new FlowableIllegalArgumentException("jobId is null");
    }
    if (log.isDebugEnabled()) {
      log.debug("Deleting job {}", jobId);
    }

    JobEntity job = commandContext.getJobEntityManager().findById(jobId);
    if (job == null) {
      throw new FlowableObjectNotFoundException("No job found with id '" + jobId + '\'', Job.class);
    }

    // We need to check if the job was locked, ie acquired by the job acquisition thread
    // This happens if the the job was already acquired, but not yet executed.
    // In that case, we can't allow to delete the job.
    if (job.getLockOwner() != null) {
      throw new FlowableException("Cannot delete job when the job is being executed. Try again later.");
    }
    return job;
  }

}
