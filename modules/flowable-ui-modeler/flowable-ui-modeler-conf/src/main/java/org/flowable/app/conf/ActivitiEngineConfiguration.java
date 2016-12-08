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
package org.flowable.app.conf;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.flowable.dmn.spring.configurator.SpringDmnEngineConfigurator;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.parse.BpmnParseHandler;
import org.flowable.form.spring.configurator.SpringFormEngineConfigurator;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ActivitiEngineConfiguration {

    private final Logger logger = LoggerFactory.getLogger(ActivitiEngineConfiguration.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private PlatformTransactionManager transactionManager;
    
    @Autowired
    private Environment environment;
    
    @Bean(name="processEngine")
    public ProcessEngineFactoryBean processEngineFactoryBean() {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(processEngineConfiguration());
        return factoryBean;
    }
    
    public ProcessEngine processEngine() {
        // Safe to call the getObject() on the @Bean annotated processEngineFactoryBean(), will be
        // the fully initialized object instanced from the factory and will NOT be created more than once
        try {
            return processEngineFactoryBean().getObject();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Bean(name="processEngineConfiguration")
    public ProcessEngineConfigurationImpl processEngineConfiguration() {
    	SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
    	processEngineConfiguration.setDataSource(dataSource);
    	processEngineConfiguration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
    	processEngineConfiguration.setTransactionManager(transactionManager);
    	processEngineConfiguration.setAsyncExecutorActivate(false);

    	// Enable safe XML. See http://activiti.org/userguide/index.html#advanced.safe.bpmn.xml
    	processEngineConfiguration.setEnableSafeBpmnXml(true);
    	
    	List<BpmnParseHandler> preParseHandlers = new ArrayList<BpmnParseHandler>();
    	processEngineConfiguration.setPreBpmnParseHandlers(preParseHandlers);
    	
    	processEngineConfiguration.setDisableIdmEngine(true); // No need to boot IDM engine for Modeler
    	
    	processEngineConfiguration.addConfigurator(new SpringFormEngineConfigurator());
    	processEngineConfiguration.addConfigurator(new SpringDmnEngineConfigurator());
    	
    	return processEngineConfiguration;
    }
    
    @Bean
    public RepositoryService repositoryService() {
    	return processEngine().getRepositoryService();
    }
    
    @Bean
    public IdmIdentityService idmIdentityService() {
      return processEngine().getIdmIdentityService();
    }
}
