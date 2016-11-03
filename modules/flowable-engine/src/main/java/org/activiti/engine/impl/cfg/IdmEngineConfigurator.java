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
package org.activiti.engine.impl.cfg;

import java.util.LinkedHashSet;
import java.util.Set;

import org.activiti.engine.cfg.AbstractProcessEngineConfigurator;
import org.activiti.idm.engine.IdmEngine;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.impl.cfg.StandaloneIdmEngineConfiguration;
import org.activiti.idm.engine.impl.interceptor.TransactionContextAwareTransactionFactory;

/**
 * @author Tijs Rademakers
 */
public class IdmEngineConfigurator extends AbstractProcessEngineConfigurator {
  
  protected IdmEngineConfiguration idmEngineConfiguration;
  
  @Override
  public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (idmEngineConfiguration == null) {
      
      idmEngineConfiguration = new StandaloneIdmEngineConfiguration();
      
      if (processEngineConfiguration.getDataSource() != null) {
        idmEngineConfiguration.setDataSource(processEngineConfiguration.getDataSource());
        
      } else if (processEngineConfiguration.getDataSourceJndiName() != null) {
        idmEngineConfiguration.setDataSourceJndiName(processEngineConfiguration.getDataSourceJndiName());
      
      } else {
        idmEngineConfiguration.setJdbcUrl(processEngineConfiguration.getJdbcUrl());
        idmEngineConfiguration.setJdbcDriver(processEngineConfiguration.getJdbcDriver());
        idmEngineConfiguration.setJdbcUsername(processEngineConfiguration.getJdbcUsername());
        idmEngineConfiguration.setJdbcPassword(processEngineConfiguration.getJdbcPassword());
      }
      
      idmEngineConfiguration.setDatabaseCatalog(processEngineConfiguration.getDatabaseCatalog());
      idmEngineConfiguration.setDatabaseSchema(processEngineConfiguration.getDatabaseSchema());
      idmEngineConfiguration.setDatabaseSchemaUpdate(processEngineConfiguration.getDatabaseSchemaUpdate());
      
      idmEngineConfiguration.setTransactionFactory(new TransactionContextAwareTransactionFactory());
      
//      Set<String> customXmlMappers = new LinkedHashSet<String>(); // Linked set -> order is important!
//      customXmlMappers.add("org/activiti/idm/db/mapping/common.xml");
//      customXmlMappers.add("org/activiti/idm/db/mapping/entity/ByteArray.xml");
//      customXmlMappers.add("org/activiti/idm/db/mapping/entity/Group.xml");
//      customXmlMappers.add("org/activiti/idm/db/mapping/entity/IdentityInfo.xml");
//      customXmlMappers.add("org/activiti/idm/db/mapping/entity/Membership.xml");
//      customXmlMappers.add("org/activiti/idm/db/mapping/entity/Property.xml");
//      customXmlMappers.add("org/activiti/idm/db/mapping/entity/TableData.xml");
//      customXmlMappers.add("org/activiti/idm/db/mapping/entity/Token.xml");
//      customXmlMappers.add("org/activiti/idm/db/mapping/entity/User.xml");
//      processEngineConfiguration.setCustomMybatisXMLMappers(customXmlMappers);
    }
    
    IdmEngine idmEngine = idmEngineConfiguration.buildIdmEngine();
    
    processEngineConfiguration.setIdmEngineInitialized(true);
    processEngineConfiguration.setIdmIdentityService(idmEngine.getIdmIdentityService());
  }
  
  @Override
  public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
    if (processEngineConfiguration.getEventDispatcher() != null) {
      idmEngineConfiguration.setEventDispatcher(processEngineConfiguration.getEventDispatcher());
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
