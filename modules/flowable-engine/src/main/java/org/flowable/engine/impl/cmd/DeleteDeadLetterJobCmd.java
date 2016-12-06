package org.flowable.engine.impl.cmd;

import java.io.Serializable;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.delegate.event.FlowableEngineEventType;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.DeadLetterJobEntity;
import org.flowable.engine.runtime.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */

public class DeleteDeadLetterJobCmd implements Command<Object>, Serializable {

  private static final Logger log = LoggerFactory.getLogger(DeleteDeadLetterJobCmd.class);
  private static final long serialVersionUID = 1L;

  protected String timerJobId;

  public DeleteDeadLetterJobCmd(String timerJobId) {
    this.timerJobId = timerJobId;
  }

  public Object execute(CommandContext commandContext) {
    DeadLetterJobEntity jobToDelete = getJobToDelete(commandContext);
    
    sendCancelEvent(jobToDelete);

    commandContext.getDeadLetterJobEntityManager().delete(jobToDelete);
    return null;
  }

  protected void sendCancelEvent(DeadLetterJobEntity jobToDelete) {
    if (Context.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
      Context.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_CANCELED, jobToDelete));
    }
  }

  protected DeadLetterJobEntity getJobToDelete(CommandContext commandContext) {
    if (timerJobId == null) {
      throw new FlowableIllegalArgumentException("jobId is null");
    }
    if (log.isDebugEnabled()) {
      log.debug("Deleting job {}", timerJobId);
    }

    DeadLetterJobEntity job = commandContext.getDeadLetterJobEntityManager().findById(timerJobId);
    if (job == null) {
      throw new FlowableObjectNotFoundException("No dead letter job found with id '" + timerJobId + "'", Job.class);
    }

    return job;
  }

}
