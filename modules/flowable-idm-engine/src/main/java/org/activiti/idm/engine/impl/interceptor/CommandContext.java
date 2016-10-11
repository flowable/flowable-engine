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
package org.activiti.idm.engine.impl.interceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.idm.engine.ActivitiIdmException;
import org.activiti.idm.engine.ActivitiIdmOptimisticLockingException;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.impl.cfg.TransactionContext;
import org.activiti.idm.engine.impl.db.DbSqlSession;
import org.activiti.idm.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.GroupEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.IdentityInfoEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.MembershipEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.PropertyEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.TableDataManager;
import org.activiti.idm.engine.impl.persistence.entity.UserEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class CommandContext {

  private static Logger log = LoggerFactory.getLogger(CommandContext.class);

  protected Command<?> command;
  protected TransactionContext transactionContext;
  protected Map<Class<?>, SessionFactory> sessionFactories;
  protected Map<Class<?>, Session> sessions = new HashMap<Class<?>, Session>();
  protected Throwable exception;
  protected IdmEngineConfiguration idmEngineConfiguration;
  protected List<CommandContextCloseListener> closeListeners;
  protected Map<String, Object> attributes; // General-purpose storing of anything during the lifetime of a command context

  public CommandContext(Command<?> command, IdmEngineConfiguration idmEngineConfiguration) {
    this.command = command;
    this.idmEngineConfiguration = idmEngineConfiguration;
    sessionFactories = idmEngineConfiguration.getSessionFactories();
    this.transactionContext = idmEngineConfiguration.getTransactionContextFactory().openTransactionContext(this);
  }

  public void close() {
    // the intention of this method is that all resources are closed properly, even if exceptions occur
    // in close or flush methods of the sessions or the transaction context.

    try {
      try {
        try {

          if (closeListeners != null) {
            try {
              for (CommandContextCloseListener listener : closeListeners) {
                listener.closing(this);
              }
            } catch (Throwable exception) {
              exception(exception);
            }
          }

          if (exception == null) {
            flushSessions();
          }

        } catch (Throwable exception) {
          exception(exception);
        } finally {

          try {
            if (exception == null) {
              transactionContext.commit();
            }
          } catch (Throwable exception) {
            exception(exception);
          }

          if (closeListeners != null) {
            try {
              for (CommandContextCloseListener listener : closeListeners) {
                listener.closed(this);
              }
            } catch (Throwable exception) {
              exception(exception);
            }
          }

          if (exception != null) {
            if (exception instanceof ActivitiIdmOptimisticLockingException) {
              // reduce log level, as normally we're not
              // interested in logging this exception
              log.debug("Optimistic locking exception : " + exception);
            } else {
              log.debug("Error while closing command context", exception);
            }

            transactionContext.rollback();
          }
        }
      } catch (Throwable exception) {
        exception(exception);
      } finally {
        closeSessions();

      }
    } catch (Throwable exception) {
      exception(exception);
    }

    // rethrow the original exception if there was one
    if (exception != null) {
      if (exception instanceof Error) {
        throw (Error) exception;
      } else if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      } else {
        throw new ActivitiIdmException("exception while executing command " + command, exception);
      }
    }
  }

  public void addCloseListener(CommandContextCloseListener commandContextCloseListener) {
    if (closeListeners == null) {
      closeListeners = new ArrayList<CommandContextCloseListener>(1);
    }
    closeListeners.add(commandContextCloseListener);
  }

  public List<CommandContextCloseListener> getCloseListeners() {
    return closeListeners;
  }

  protected void flushSessions() {
    for (Session session : sessions.values()) {
      session.flush();
    }
  }

  protected void closeSessions() {
    for (Session session : sessions.values()) {
      try {
        session.close();
      } catch (Throwable exception) {
        exception(exception);
      }
    }
  }

  public void exception(Throwable exception) {
    if (this.exception == null) {
      this.exception = exception;

    } else {
      log.error("masked exception in command context. for root cause, see below as it will be rethrown later.", exception);
    }
  }

  public void addAttribute(String key, Object value) {
    if (attributes == null) {
      attributes = new HashMap<String, Object>(1);
    }
    attributes.put(key, value);
  }

  public Object getAttribute(String key) {
    if (attributes != null) {
      return attributes.get(key);
    }
    return null;
  }

  @SuppressWarnings({ "unchecked" })
  public <T> T getSession(Class<T> sessionClass) {
    Session session = sessions.get(sessionClass);
    if (session == null) {
      SessionFactory sessionFactory = sessionFactories.get(sessionClass);
      if (sessionFactory == null) {
        throw new ActivitiIdmException("no session factory configured for " + sessionClass.getName());
      }
      session = sessionFactory.openSession(this);
      sessions.put(sessionClass, session);
    }

    return (T) session;
  }

  public Map<Class<?>, SessionFactory> getSessionFactories() {
    return sessionFactories;
  }

  public DbSqlSession getDbSqlSession() {
    return getSession(DbSqlSession.class);
  }
  
  public ByteArrayEntityManager getByteArrayEntityManager() {
    return idmEngineConfiguration.getByteArrayEntityManager();
  }
  
  public GroupEntityManager getGroupEntityManager() {
    return idmEngineConfiguration.getGroupEntityManager();
  }
  
  public IdentityInfoEntityManager getIdentityInfoEntityManager() {
    return idmEngineConfiguration.getIdentityInfoEntityManager();
  }
  
  public MembershipEntityManager getMembershipEntityManager() {
    return idmEngineConfiguration.getMembershipEntityManager();
  }
  
  public PropertyEntityManager getPropertyEntityManager() {
    return idmEngineConfiguration.getPropertyEntityManager();
  }

  public UserEntityManager getUserEntityManager() {
    return idmEngineConfiguration.getUserEntityManager();
  }
  
  public TableDataManager getTableDataManager() {
    return idmEngineConfiguration.getTableDataManager();
  }

  // getters and setters
  // //////////////////////////////////////////////////////
  
  public IdmEngineConfiguration getIdmEngineConfiguration() {
    return idmEngineConfiguration;
  }

  public TransactionContext getTransactionContext() {
    return transactionContext;
  }

  public Command<?> getCommand() {
    return command;
  }

  public Map<Class<?>, Session> getSessions() {
    return sessions;
  }

  public Throwable getException() {
    return exception;
  }
}
