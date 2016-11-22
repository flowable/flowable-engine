package org.activiti.engine.impl.cmd;

import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;

public class UnacquireOwnedJobsCmd implements Command<Void> {

  private final String lockOwner;
  private final String tenantId;
  
  public UnacquireOwnedJobsCmd(String lockOwner, String tenantId) {
    this.lockOwner = lockOwner;
    this.tenantId = tenantId;
  }

  @Override
  public Void execute(CommandContext commandContext) {
    commandContext.getJobEntityManager().unacquireOwnedJobs(lockOwner, tenantId);
    return null;
  }
}
