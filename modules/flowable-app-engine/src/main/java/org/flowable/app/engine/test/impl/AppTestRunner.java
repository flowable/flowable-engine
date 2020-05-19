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
package org.flowable.app.engine.test.impl;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDeploymentBuilder;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.test.AppDeployment;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
import org.junit.Ignore;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class AppTestRunner extends BlockJUnit4ClassRunner {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AppTestRunner.class);
    
    protected static AppEngineConfiguration appEngineConfiguration;

    protected static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = Arrays.asList(
            "ACT_GE_PROPERTY",
            "ACT_ID_PROPERTY",
            "ACT_APP_DATABASECHANGELOG",
            "ACT_APP_DATABASECHANGELOGLOCK",
            "ACT_CMMN_DATABASECHANGELOG",
            "ACT_CMMN_DATABASECHANGELOGLOCK",
            "ACT_FO_DATABASECHANGELOG",
            "ACT_FO_DATABASECHANGELOGLOCK",
            "FLW_EV_DATABASECHANGELOG",
            "FLW_EV_DATABASECHANGELOGLOCK"
    );

    public AppTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }
    
    public static AppEngineConfiguration getAppEngineConfiguration() {
        return AppTestRunner.appEngineConfiguration;
    }

    public static void setAppEngineConfiguration(AppEngineConfiguration appEngineConfiguration) {
        AppTestRunner.appEngineConfiguration = appEngineConfiguration;
    }
    
    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        String deploymentId = null;
        if (method.getAnnotation(Ignore.class) == null && method.getAnnotation(AppDeployment.class) != null) {
            deploymentId = deployAppDefinition(method);
        }
        
        super.runChild(method, notifier);
        
        if (deploymentId != null) {
            deleteDeployment(deploymentId);
        }
        assertDatabaseEmpty(method);
    }

    protected String deployAppDefinition(FrameworkMethod method) {
        try {
            LOGGER.debug("annotation @AppDeployment creates deployment for {}.{}", method.getMethod().getDeclaringClass().getSimpleName(), method.getName());
    
            if (appEngineConfiguration == null) {
                throw new FlowableException("No cached App engine found.");
            }
            AppRepositoryService repositoryService = appEngineConfiguration.getAppRepositoryService();
            AppDeploymentBuilder deploymentBuilder = repositoryService
                    .createDeployment()
                    .name(method.getMethod().getDeclaringClass().getSimpleName() + "." + method.getName());
            
            AppDeployment deploymentAnnotation = method.getAnnotation(AppDeployment.class);
            String[] resources = deploymentAnnotation.resources();
            
            if (resources.length == 0) {
                resources = new String[] { getAppDefinitionResource(method) };
            }
    
            for (String resource : resources) {
                deploymentBuilder.addClasspathResource(resource);
            }
            
            if (StringUtils.isNotEmpty(deploymentAnnotation.tenantId())) {
                deploymentBuilder.tenantId(deploymentAnnotation.tenantId());
            }
            
            return deploymentBuilder.deploy().getId();
            
        } catch (Exception e) {
            LOGGER.error("Error while deploying app definition", e);
        }
        
        return null;
    }
    
    protected String getAppDefinitionResource(FrameworkMethod method) {
        String className = method.getMethod().getDeclaringClass().getName().replace('.', '/');
        String methodName = method.getName();
        String resource = className + "." + methodName + ".app";
        return resource;
    }
    
    protected void deleteDeployment(String deploymentId) {
        appEngineConfiguration.getAppRepositoryService().deleteDeployment(deploymentId, true);
    }
    
    protected void assertDatabaseEmpty(FrameworkMethod method) {
        EnsureCleanDbUtils.assertAndEnsureCleanDb(
                getTestClass().getName() + "." + method.getName(),
                LOGGER,
                appEngineConfiguration,
                TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK,
                true,
                null
        );
    }

}
