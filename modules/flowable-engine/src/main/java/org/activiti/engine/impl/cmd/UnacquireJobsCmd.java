package org.activiti.engine.impl.cmd;

import java.util.List;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.JobEntity;

/**
 *
 * @author Marcus Klimstra
 */
public class UnacquireJobsCmd implements Command<Void> {

  private final List<JobEntity> jobs;
  
  public UnacquireJobsCmd(List<JobEntity> jobs) {
    this.jobs = jobs;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    for (JobEntity job : jobs) {
      commandContext.getJobEntityManager().unacquireJob(job.getId());
    }
    return null;
  }
}
