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

import org.activiti.engine.common.impl.interceptor.AbstractCommandContext;
import org.activiti.engine.common.impl.interceptor.BaseCommandContextCloseListener;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.impl.db.DbSqlSession;
import org.activiti.idm.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.CapabilityEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.GroupEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.IdentityInfoEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.MembershipEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.PropertyEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.TableDataManager;
import org.activiti.idm.engine.impl.persistence.entity.TokenEntityManager;
import org.activiti.idm.engine.impl.persistence.entity.UserEntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class CommandContext extends AbstractCommandContext {

  private static Logger log = LoggerFactory.getLogger(CommandContext.class);

  protected IdmEngineConfiguration idmEngineConfiguration;
  
  public CommandContext(Command<?> command, IdmEngineConfiguration idmEngineConfiguration) {
    super(command);
    this.idmEngineConfiguration = idmEngineConfiguration;
    sessionFactories = idmEngineConfiguration.getSessionFactories();
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
  
  public TokenEntityManager getTokenEntityManager() {
    return idmEngineConfiguration.getTokenEntityManager();
  }

  public UserEntityManager getUserEntityManager() {
    return idmEngineConfiguration.getUserEntityManager();
  }
  
  public CapabilityEntityManager getCapabilityEntityManager() {
    return idmEngineConfiguration.getCapabilityEntityManager();
  }
  
  public TableDataManager getTableDataManager() {
    return idmEngineConfiguration.getTableDataManager();
  }
  
  // getters and setters
  // //////////////////////////////////////////////////////
  
  public IdmEngineConfiguration getIdmEngineConfiguration() {
    return idmEngineConfiguration;
  }
}
