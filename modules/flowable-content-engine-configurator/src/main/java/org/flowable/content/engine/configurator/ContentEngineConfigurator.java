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
package org.flowable.content.engine.configurator;

import javax.sql.DataSource;

import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.cfg.StandaloneContentEngineConfiguration;
import org.flowable.engine.cfg.AbstractProcessEngineConfigurator;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.transaction.TransactionContextAwareDataSource;
import org.flowable.engine.common.impl.transaction.TransactionContextAwareTransactionFactory;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ContentEngineConfigurator extends AbstractProcessEngineConfigurator {
  
  protected ContentEngineConfiguration contentEngineConfiguration;
  
  @Override
  public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (contentEngineConfiguration == null) {
      contentEngineConfiguration = new StandaloneContentEngineConfiguration();
    
      if (processEngineConfiguration.getDataSource() != null) {
        DataSource originalDatasource = processEngineConfiguration.getDataSource();
        if (processEngineConfiguration.isTransactionsExternallyManaged()) {
          contentEngineConfiguration.setDataSource(originalDatasource);
        } else {
          contentEngineConfiguration.setDataSource(new TransactionContextAwareDataSource(originalDatasource));
        }
        
      } else {
        throw new FlowableException("A datasource is required for initializing the Content engine ");
      }
      
      contentEngineConfiguration.setDatabaseCatalog(processEngineConfiguration.getDatabaseCatalog());
      contentEngineConfiguration.setDatabaseSchema(processEngineConfiguration.getDatabaseSchema());
      contentEngineConfiguration.setDatabaseSchemaUpdate(processEngineConfiguration.getDatabaseSchemaUpdate());
      contentEngineConfiguration.setDatabaseTablePrefix(processEngineConfiguration.getDatabaseTablePrefix());
      contentEngineConfiguration.setDatabaseWildcardEscapeCharacter(processEngineConfiguration.getDatabaseWildcardEscapeCharacter());
      
      if (processEngineConfiguration.isTransactionsExternallyManaged()) {
        contentEngineConfiguration.setTransactionsExternallyManaged(true);
       } else {
         contentEngineConfiguration.setTransactionFactory(
             new TransactionContextAwareTransactionFactory<org.flowable.content.engine.impl.cfg.TransactionContext>(
                   org.flowable.content.engine.impl.cfg.TransactionContext.class));
       }
      
    }
    
    ContentEngine contentEngine = initContentEngine();
    processEngineConfiguration.setContentEngineInitialized(true);
    processEngineConfiguration.setContentService(contentEngine.getContentService());
  }

  protected synchronized ContentEngine initContentEngine() {
    if (contentEngineConfiguration == null) {
      throw new FlowableException("ContentEngineConfiguration is required");
    }
    
    return contentEngineConfiguration.buildContentEngine();
  }

  public ContentEngineConfiguration getContentEngineConfiguration() {
    return contentEngineConfiguration;
  }

  public ContentEngineConfigurator setContentEngineConfiguration(ContentEngineConfiguration contentEngineConfiguration) {
    this.contentEngineConfiguration = contentEngineConfiguration;
    return this;
  }

}
