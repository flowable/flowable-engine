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
package org.activiti.dmn.spring.configurator;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.activiti.dmn.engine.DmnEngine;
import org.activiti.dmn.engine.deployer.DmnDeployer;
import org.activiti.dmn.spring.SpringDmnEngineConfiguration;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.cfg.AbstractProcessEngineConfigurator;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.deploy.Deployer;
import org.activiti.spring.SpringProcessEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringDmnEngineConfigurator extends AbstractProcessEngineConfigurator {
  
  protected SpringDmnEngineConfiguration dmnEngineConfiguration;
  
  @Override
  public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    
    // Custom deployers need to be added before the process engine boots
    List<Deployer> deployers = null;
    if (processEngineConfiguration.getCustomPostDeployers() != null) {
      deployers = processEngineConfiguration.getCustomPostDeployers();
    } else {
      deployers = new ArrayList<Deployer>();
    }
    deployers.add(new DmnDeployer());
    processEngineConfiguration.setCustomPostDeployers(deployers);
    
  }
  
  @Override
  public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (dmnEngineConfiguration == null) {
      dmnEngineConfiguration = new SpringDmnEngineConfiguration();
    }
    
    if (processEngineConfiguration.getDataSource() != null) {
      DataSource originalDatasource = processEngineConfiguration.getDataSource();
      dmnEngineConfiguration.setDataSource(originalDatasource);
      
    } else {
      throw new ActivitiException("A datasource is required for initializing the DMN engine ");
    }
    
    dmnEngineConfiguration.setTransactionManager(((SpringProcessEngineConfiguration) processEngineConfiguration).getTransactionManager());
    
    dmnEngineConfiguration.setDatabaseCatalog(processEngineConfiguration.getDatabaseCatalog());
    dmnEngineConfiguration.setDatabaseSchema(processEngineConfiguration.getDatabaseSchema());
    dmnEngineConfiguration.setDatabaseSchemaUpdate(processEngineConfiguration.getDatabaseSchemaUpdate());
    
    DmnEngine dmnEngine = initDmnEngine();
    
    processEngineConfiguration.setDmnEngineInitialized(true);
    processEngineConfiguration.setDmnEngineRepositoryService(dmnEngine.getDmnRepositoryService());
    processEngineConfiguration.setDmnEngineRuleService(dmnEngine.getDmnRuleService());
  }

  protected synchronized DmnEngine initDmnEngine() {
    if (dmnEngineConfiguration == null) {
      throw new ActivitiException("DMnEngineConfiguration is required");
    }
    
    return dmnEngineConfiguration.buildDmnEngine();
  }

  public SpringDmnEngineConfiguration getDmnEngineConfiguration() {
    return dmnEngineConfiguration;
  }

  public SpringDmnEngineConfigurator setDmnEngineConfiguration(SpringDmnEngineConfiguration dmnEngineConfiguration) {
    this.dmnEngineConfiguration = dmnEngineConfiguration;
    return this;
  }

}
