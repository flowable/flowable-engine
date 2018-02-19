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
package org.flowable.spring.boot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.spring.ProcessEngineFactoryBean;
import org.flowable.spring.SpringAsyncExecutor;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provides sane definitions for the various beans required to be productive with Flowable in Spring.
 *
 * @author Josh Long
 */
public abstract class AbstractProcessEngineConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessEngineConfiguration.class);

    public ProcessEngineFactoryBean springProcessEngineBean(SpringProcessEngineConfiguration configuration) {
        ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();
        processEngineFactoryBean.setProcessEngineConfiguration(configuration);
        return processEngineFactoryBean;
    }

    public SpringProcessEngineConfiguration processEngineConfigurationBean(Resource[] processDefinitions,
            DataSource dataSource,
            PlatformTransactionManager transactionManager,
            SpringAsyncExecutor springAsyncExecutor)
            throws IOException {

        SpringProcessEngineConfiguration engine = new SpringProcessEngineConfiguration();
        if (processDefinitions != null && processDefinitions.length > 0) {
            engine.setDeploymentResources(processDefinitions);
        }
        engine.setDataSource(dataSource);
        engine.setTransactionManager(transactionManager);

        if (null != springAsyncExecutor) {
            engine.setAsyncExecutor(springAsyncExecutor);
        }

        return engine;
    }

    public List<Resource> discoverProcessDefinitionResources(ResourcePatternResolver applicationContext, String prefix, List<String> suffixes, boolean checkPDs) throws IOException {
        if (checkPDs) {

            List<Resource> result = new ArrayList<>();
            for (String suffix : suffixes) {
                String path = prefix + suffix;
                Resource[] resources = applicationContext.getResources(path);
                if (resources != null && resources.length > 0) {
                    Collections.addAll(result, resources);
                }
            }

            if (result.isEmpty()) {
                LOGGER.info("No process definitions were found for autodeployment");
            }

            return result;
        }
        return new ArrayList<>();
    }

    public RuntimeService runtimeServiceBean(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    public RepositoryService repositoryServiceBean(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    public TaskService taskServiceBean(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    public HistoryService historyServiceBean(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    public ManagementService managementServiceBeanBean(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }

    public FormService formServiceBean(ProcessEngine processEngine) {
        return processEngine.getFormService();
    }

    public IdentityService identityServiceBean(ProcessEngine processEngine) {
        return processEngine.getIdentityService();
    }

    public IdmIdentityService idmIdentityServiceBean(ProcessEngine processEngine) {
        return ((IdmEngineConfiguration) processEngine.getProcessEngineConfiguration().getEngineConfigurations()
                    .get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG)).getIdmIdentityService();
    }

}
