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
package org.activiti.form.engine.configurator;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.cfg.AbstractProcessEngineConfigurator;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.deploy.Deployer;
import org.activiti.engine.impl.transaction.TransactionContextAwareDataSource;
import org.activiti.engine.impl.transaction.TransactionContextAwareTransactionFactory;
import org.activiti.form.engine.FormEngine;
import org.activiti.form.engine.FormEngineConfiguration;
import org.activiti.form.engine.deployer.FormDeployer;
import org.activiti.form.engine.impl.cfg.StandaloneFormEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FormEngineConfigurator extends AbstractProcessEngineConfigurator {
  
  protected FormEngineConfiguration formEngineConfiguration;
  
  @Override
  public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    
    // Custom deployers need to be added before the process engine boots
    List<Deployer> deployers = null;
    if (processEngineConfiguration.getCustomPostDeployers() != null) {
      deployers = processEngineConfiguration.getCustomPostDeployers();
    } else {
      deployers = new ArrayList<Deployer>();
    }
    deployers.add(new FormDeployer());
    processEngineConfiguration.setCustomPostDeployers(deployers);
    
  }
  
  @Override
  public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (formEngineConfiguration == null) {
      formEngineConfiguration = new StandaloneFormEngineConfiguration();
    
      if (processEngineConfiguration.getDataSource() != null) {
        DataSource originalDatasource = processEngineConfiguration.getDataSource();
        if (processEngineConfiguration.isTransactionsExternallyManaged()) {
          formEngineConfiguration.setDataSource(originalDatasource);
        } else {
          formEngineConfiguration.setDataSource(new TransactionContextAwareDataSource(originalDatasource));
        }
        
      } else {
        throw new ActivitiException("A datasource is required for initializing the Form engine ");
      }
      
      formEngineConfiguration.setDatabaseCatalog(processEngineConfiguration.getDatabaseCatalog());
      formEngineConfiguration.setDatabaseSchema(processEngineConfiguration.getDatabaseSchema());
      formEngineConfiguration.setDatabaseSchemaUpdate(processEngineConfiguration.getDatabaseSchemaUpdate());
      
      if (processEngineConfiguration.isTransactionsExternallyManaged()) {
        formEngineConfiguration.setTransactionsExternallyManaged(true);
       } else {
        formEngineConfiguration.setTransactionFactory(
             new TransactionContextAwareTransactionFactory<org.activiti.idm.engine.impl.cfg.TransactionContext>(
                   org.activiti.idm.engine.impl.cfg.TransactionContext.class));
       }
      
    }
    
    FormEngine formEngine = initFormEngine();
    processEngineConfiguration.setFormEngineInitialized(true);
    processEngineConfiguration.setFormEngineRepositoryService(formEngine.getFormRepositoryService());
    processEngineConfiguration.setFormEngineFormService(formEngine.getFormService());
  }

  protected synchronized FormEngine initFormEngine() {
    if (formEngineConfiguration == null) {
      throw new ActivitiException("FormEngineConfiguration is required");
    }
    
    return formEngineConfiguration.buildFormEngine();
  }

  public FormEngineConfiguration getFormEngineConfiguration() {
    return formEngineConfiguration;
  }

  public FormEngineConfigurator setFormEngineConfiguration(FormEngineConfiguration formEngineConfiguration) {
    this.formEngineConfiguration = formEngineConfiguration;
    return this;
  }

}
