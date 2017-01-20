package org.activiti.idm.engine.impl.interceptor;

import org.activiti.engine.common.impl.interceptor.BaseCommandContextCloseListener;

/**
 * A listener that can be used to be notified of lifecycle events of the command context.
 * 
 * @author Joram Barrez
 */
public interface CommandContextCloseListener extends BaseCommandContextCloseListener<CommandContext> {

  void closing(CommandContext commandContext);

  void closed(CommandContext commandContext);

}
