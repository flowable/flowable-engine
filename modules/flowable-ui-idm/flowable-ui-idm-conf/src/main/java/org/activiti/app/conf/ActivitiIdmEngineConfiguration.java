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
package org.activiti.app.conf;

import javax.sql.DataSource;

import org.activiti.engine.runtime.Clock;
import org.activiti.idm.api.IdmIdentityService;
import org.activiti.idm.api.IdmManagementService;
import org.activiti.idm.engine.IdmEngine;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.spring.SpringIdmEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@ComponentScan(basePackages= {
		"org.activiti.app.runtime.activiti",
		"org.activiti.app.extension.conf", // For custom configuration classes
		"org.activiti.app.extension.bean" // For custom beans (delegates etc.)
})
public class ActivitiIdmEngineConfiguration {

    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Bean(name="idmEngine")
    public IdmEngine idmEngine() {
        return idmEngineConfiguration().buildIdmEngine();
    }
    
    @Bean(name="idmEngineConfiguration")
    public IdmEngineConfiguration idmEngineConfiguration() {
      SpringIdmEngineConfiguration idmEngineConfiguration = new SpringIdmEngineConfiguration();
      idmEngineConfiguration.setDataSource(dataSource);
      idmEngineConfiguration.setDatabaseSchemaUpdate(IdmEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
      idmEngineConfiguration.setTransactionManager(transactionManager);
    	
    	return idmEngineConfiguration;
    }
    
    @Bean(name="clock")
    @DependsOn("idmEngine")
    public Clock getClock() {
    	return idmEngineConfiguration().getClock();
    }
    
    @Bean
    public IdmIdentityService idmIdentityService() {
    	return idmEngine().getIdmIdentityService();
    }
    
    @Bean
    public IdmManagementService idmManagementService() {
    	return idmEngine().getIdmManagementService();
    }
}
