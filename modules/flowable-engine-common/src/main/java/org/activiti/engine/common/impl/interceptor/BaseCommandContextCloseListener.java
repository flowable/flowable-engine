package org.activiti.engine.common.impl.interceptor;

/**
 * A listener that can be used to be notified of lifecycle events of the {@link CommandContext}.
 * 
 * @author Joram Barrez
 */
public interface BaseCommandContextCloseListener<C extends AbstractCommandContext> {

  /**
   * Called when the {@link CommandContext} is being closed, but no 'close logic' has been executed.
   * 
   * At this point, the {@link TransactionContext} (if applicable) has not yet been committed/rolledback 
   * and none of the {@link Session} instances have been flushed.
   * 
   * If an exception happens and it is not caught in this method:
   * - The {@link Session} instances will *not* be flushed
   * - The {@link TransactionContext} will be rolled back (if applicable) 
   */
  void closing(C commandContext);
  
  /**
   * Called when the {@link Session} have been successfully flushed.
   * When an exception happened during the flushing of the sessions, this method will not be called.
   * 
   * If an exception happens and it is not caught in this method:
   * - The {@link Session} instances will *not* be flushed
   * - The {@link TransactionContext} will be rolled back (if applicable) 
   */
  void afterSessionsFlush(C commandContext);

  /**
   * Called when the {@link CommandContext} is successfully closed.
   * 
   * At this point, the {@link TransactionContext} (if applicable) has been successfully committed
   * and no rollback has happened. All {@link Session} instances have been closed.
   * 
   * Note that throwing an exception here does *not* affect the transaction. 
   * The {@link CommandContext} will log the exception though.
   */
  void closed(C commandContext);
  
  /**
   * Called when the {@link CommandContext} has not been successully closed due to an exception that happened.
   * 
   * Note that throwing an exception here does *not* affect the transaction. 
   * The {@link CommandContext} will log the exception though.
   */
  void closeFailure(C commandContext);

}
