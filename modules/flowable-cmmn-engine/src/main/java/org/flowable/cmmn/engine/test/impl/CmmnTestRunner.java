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
package org.flowable.cmmn.engine.test.impl;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeployer;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.FlowableException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CmmnTestRunner extends BlockJUnit4ClassRunner {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnTestRunner.class);
    
    protected static CmmnEngineConfiguration cmmnEngineConfiguration;

    public CmmnTestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }
    
    public static CmmnEngineConfiguration getCmmnEngineConfiguration() {
        return CmmnTestRunner.cmmnEngineConfiguration;
    }

    public static void setCmmnEngineConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        CmmnTestRunner.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }
    
    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        String deploymentId = null;
        if (method.getAnnotation(Ignore.class) == null && method.getAnnotation(CmmnDeployment.class) != null) {
            deploymentId = deployCmmnDefinition(method);
        }
        
        super.runChild(method, notifier);
        
        if (deploymentId != null) {
            deleteDeployment(deploymentId);
        }
        assertDatabaseEmpty(method);
    }

    protected String deployCmmnDefinition(FrameworkMethod method) {
        try {
            LOGGER.debug("annotation @CmmnDeployment creates deployment for {}.{}", method.getMethod().getDeclaringClass().getSimpleName(), method.getName());
    
            if (cmmnEngineConfiguration == null) {
                throw new FlowableException("No cached CMMN engine found.");
            }
            CmmnRepositoryService repositoryService = cmmnEngineConfiguration.getCmmnRepositoryService();
            CmmnDeploymentBuilder deploymentBuilder = repositoryService
                    .createDeployment()
                    .name(method.getMethod().getDeclaringClass().getSimpleName() + "." + method.getName());
            
            CmmnDeployment deploymentAnnotation = method.getAnnotation(CmmnDeployment.class);
            String[] resources = deploymentAnnotation.resources();
            
            if (resources.length == 0) {
                resources = new String[] { getCmmnDefinitionResource(method) };
            }
    
            for (String resource : resources) {
                deploymentBuilder.addClasspathResource(resource);
            }
            
            if (StringUtils.isNotEmpty(deploymentAnnotation.tenantId())) {
                deploymentBuilder.tenantId(deploymentAnnotation.tenantId());
            }
            
            return deploymentBuilder.deploy().getId();
            
        } catch (Exception e) {
            LOGGER.error("Error while deploying case definition", e);
        }
        
        return null;
    }
    
    protected String getCmmnDefinitionResource(FrameworkMethod method) {
        String className = method.getMethod().getDeclaringClass().getName().replace('.', '/');
        String methodName = method.getName();
        for (String suffix : CmmnDeployer.CMMN_RESOURCE_SUFFIXES) {
            String resource = className + "." + methodName + "." + suffix;
            if (CmmnTestRunner.class.getClassLoader().getResource(resource) != null) {
                return resource;
            }
        }
        return className + "." + method.getName() + ".cmmn";
    }
    
    protected void deleteDeployment(String deploymentId) {
        cmmnEngineConfiguration.getCmmnRepositoryService().deleteDeployment(deploymentId, true);
    }
    
    protected void assertDatabaseEmpty(FrameworkMethod method) {
        Map<String, Long> tableCounts = cmmnEngineConfiguration.getCmmnManagementService().getTableCounts();
        
        StringBuilder outputMessage = new StringBuilder();
        for (String table : tableCounts.keySet()) {
            long count = tableCounts.get(table);
            if (count != 0) {
                outputMessage.append("  ").append(table).append(": ").append(count).append(" record(s) ");
            }
        }
        
        if (outputMessage.length() > 0) {
            outputMessage.insert(0, "DB not clean for test " + getTestClass().getName() + "." + method.getName() + ": \n");
            LOGGER.error("\n");
            LOGGER.error(outputMessage.toString());
            Assert.fail(outputMessage.toString());

        } else {
            LOGGER.info("database was clean");
            
        }
    }

}
