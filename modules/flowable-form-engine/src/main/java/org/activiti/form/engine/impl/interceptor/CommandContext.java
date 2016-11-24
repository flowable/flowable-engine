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
package org.activiti.form.engine.impl.interceptor;

import java.util.ArrayList;

import org.activiti.engine.common.impl.interceptor.AbstractCommandContext;
import org.activiti.engine.common.impl.interceptor.BaseCommandContextCloseListener;
import org.activiti.form.engine.FormEngineConfiguration;
import org.activiti.form.engine.impl.db.DbSqlSession;
import org.activiti.form.engine.impl.persistence.entity.FormDefinitionEntityManager;
import org.activiti.form.engine.impl.persistence.entity.FormDeploymentEntityManager;
import org.activiti.form.engine.impl.persistence.entity.FormInstanceEntityManager;
import org.activiti.form.engine.impl.persistence.entity.ResourceEntityManager;
import org.activiti.form.engine.impl.persistence.entity.TableDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class CommandContext extends AbstractCommandContext {

  private static Logger log = LoggerFactory.getLogger(CommandContext.class);

  protected FormEngineConfiguration formEngineConfiguration;
  
  public CommandContext(Command<?> command, FormEngineConfiguration formEngineConfiguration) {
    super(command);
    this.formEngineConfiguration = formEngineConfiguration;
    sessionFactories = formEngineConfiguration.getSessionFactories();
  }

  public void addCloseListener(CommandContextCloseListener commandContextCloseListener) {
    if (closeListeners == null) {
      closeListeners = new ArrayList<BaseCommandContextCloseListener<AbstractCommandContext>>(1);
    }
    closeListeners.add((BaseCommandContextCloseListener) commandContextCloseListener);
  }

  public DbSqlSession getDbSqlSession() {
    return getSession(DbSqlSession.class);
  }

  public FormDeploymentEntityManager getDeploymentEntityManager() {
    return formEngineConfiguration.getDeploymentEntityManager();
  }
  
  public FormDefinitionEntityManager getFormDefinitionEntityManager() {
    return formEngineConfiguration.getFormDefinitionEntityManager();
  }

  public ResourceEntityManager getResourceEntityManager() {
    return formEngineConfiguration.getResourceEntityManager();
  }
  
  public FormInstanceEntityManager getFormInstanceEntityManager() {
    return formEngineConfiguration.getFormInstanceEntityManager();
  }
  
  public TableDataManager getTableDataManager() {
    return formEngineConfiguration.getTableDataManager();
  }

  // getters and setters
  // //////////////////////////////////////////////////////
  
  public FormEngineConfiguration getFormEngineConfiguration() {
    return formEngineConfiguration;
  }
}
