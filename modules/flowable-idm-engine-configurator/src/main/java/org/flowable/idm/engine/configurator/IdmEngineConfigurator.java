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
package org.flowable.idm.engine.configurator;

import org.flowable.engine.cfg.AbstractProcessEngineConfigurator;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;

/**
 * @author Tijs Rademakers
 */
public class IdmEngineConfigurator extends AbstractProcessEngineConfigurator {
  
  protected static IdmEngine idmEngine;
  protected IdmEngineConfiguration idmEngineConfiguration;
  
  @Override
  public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    initIdmEngine();
    
    processEngineConfiguration.setIdmEngineInitialized(true);
    processEngineConfiguration.setIdmIdentityService(idmEngine.getIdmIdentityService());
  }

  protected synchronized void initIdmEngine() {
    if (idmEngine == null) {
      if (idmEngineConfiguration == null) {
        throw new FlowableException("IdmEngineConfiguration is required");
      }
      
      idmEngine = idmEngineConfiguration.buildIdmEngine();
    }
  }

  public IdmEngineConfiguration getIdmEngineConfiguration() {
    return idmEngineConfiguration;
  }

  public IdmEngineConfigurator setIdmEngineConfiguration(IdmEngineConfiguration idmEngineConfiguration) {
    this.idmEngineConfiguration = idmEngineConfiguration;
    return this;
  }

}
