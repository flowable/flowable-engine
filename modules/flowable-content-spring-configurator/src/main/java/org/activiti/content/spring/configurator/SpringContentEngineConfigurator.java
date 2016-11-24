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
package org.activiti.content.spring.configurator;

import javax.sql.DataSource;

import org.activiti.content.engine.ContentEngine;
import org.activiti.content.spring.SpringContentEngineConfiguration;
import org.activiti.engine.cfg.AbstractProcessEngineConfigurator;
import org.activiti.engine.common.api.ActivitiException;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.spring.SpringProcessEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringContentEngineConfigurator extends AbstractProcessEngineConfigurator {
  
  protected SpringContentEngineConfiguration contentEngineConfiguration;
  
  @Override
  public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (contentEngineConfiguration == null) {
      contentEngineConfiguration = new SpringContentEngineConfiguration();
    }
    
    if (processEngineConfiguration.getDataSource() != null) {
      DataSource originalDatasource = processEngineConfiguration.getDataSource();
      contentEngineConfiguration.setDataSource(originalDatasource);
      
    } else {
      throw new ActivitiException("A datasource is required for initializing the Content engine ");
    }
    
    contentEngineConfiguration.setTransactionManager(((SpringProcessEngineConfiguration) processEngineConfiguration).getTransactionManager());
    
    contentEngineConfiguration.setDatabaseCatalog(processEngineConfiguration.getDatabaseCatalog());
    contentEngineConfiguration.setDatabaseSchema(processEngineConfiguration.getDatabaseSchema());
    contentEngineConfiguration.setDatabaseSchemaUpdate(processEngineConfiguration.getDatabaseSchemaUpdate());
    
    ContentEngine contentEngine = initContentEngine();
    processEngineConfiguration.setContentEngineInitialized(true);
    processEngineConfiguration.setContentService(contentEngine.getContentService());
  }

  protected synchronized ContentEngine initContentEngine() {
    if (contentEngineConfiguration == null) {
      throw new ActivitiException("ContentEngineConfiguration is required");
    }
    
    return contentEngineConfiguration.buildContentEngine();
  }

  public SpringContentEngineConfiguration getContentEngineConfiguration() {
    return contentEngineConfiguration;
  }

  public SpringContentEngineConfigurator setContentEngineConfiguration(SpringContentEngineConfiguration contentEngineConfiguration) {
    this.contentEngineConfiguration = contentEngineConfiguration;
    return this;
  }

}
