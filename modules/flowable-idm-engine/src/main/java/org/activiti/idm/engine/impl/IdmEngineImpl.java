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
package org.activiti.idm.engine.impl;

import org.activiti.idm.api.IdmIdentityService;
import org.activiti.idm.api.IdmManagementService;
import org.activiti.idm.engine.IdmEngine;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.IdmEngines;
import org.activiti.idm.engine.impl.interceptor.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class IdmEngineImpl implements IdmEngine {

  private static Logger log = LoggerFactory.getLogger(IdmEngineImpl.class);

  protected String name;
  protected IdmIdentityService identityService;
  protected IdmManagementService managementService;
  protected IdmEngineConfiguration engineConfiguration;
  protected CommandExecutor commandExecutor;

  public IdmEngineImpl(IdmEngineConfiguration engineConfiguration) {
    this.engineConfiguration = engineConfiguration;
    this.name = engineConfiguration.getEngineName();
    this.identityService = engineConfiguration.getIdmIdentityService();
    this.managementService = engineConfiguration.getIdmManagementService();
    this.commandExecutor = engineConfiguration.getCommandExecutor();
    
    if (engineConfiguration.isUsingRelationalDatabase() && engineConfiguration.getDatabaseSchemaUpdate() != null) {
      commandExecutor.execute(engineConfiguration.getSchemaCommandConfig(), new SchemaOperationsIdmEngineBuild());
    }

    if (name == null) {
      log.info("default activiti IdmEngine created");
    } else {
      log.info("IdmEngine {} created", name);
    }

    IdmEngines.registerIdmEngine(this);
  }

  public void close() {
    IdmEngines.unregister(this);
  }

  // getters and setters
  // //////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public IdmIdentityService getIdmIdentityService() {
    return identityService;
  }
  
  public IdmManagementService getIdmManagementService() {
    return managementService;
  }

  public IdmEngineConfiguration getIdmEngineConfiguration() {
    return engineConfiguration;
  }
}
