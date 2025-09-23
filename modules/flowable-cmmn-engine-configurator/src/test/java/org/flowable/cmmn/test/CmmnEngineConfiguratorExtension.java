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
package org.flowable.cmmn.test;

import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.CmmnEngines;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.impl.test.TestHelper;
import org.flowable.engine.test.ConfigurationResource;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.FlowableExtension;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Filip Hrisafov
 */
public class CmmnEngineConfiguratorExtension implements AfterTestExecutionCallback, AfterEachCallback, BeforeEachCallback, ParameterResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnEngineConfiguratorExtension.class);
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(CmmnEngineConfiguratorExtension.class);

    private static final String DEPLOYMENT_ID_KEY = "deploymentIdFromDeploymentAnnotation";
    private static final String CMMN_DEPLOYMENT_ID_KEY = "cmmnDeploymentIdFromCmmnDeploymentAnnotation";

    @Override
    public void beforeEach(ExtensionContext context) {
        ProcessEngine processEngine = getProcessEngine(context);
        AnnotationSupport.findAnnotation(context.getTestMethod(), Deployment.class)
                .ifPresent(deployment -> {
                    String deploymentIdFromDeploymentAnnotation = TestHelper.annotationDeploymentSetUp(processEngine, context.getRequiredTestMethod(),
                            deployment);
                    getStore(context).put(context.getUniqueId() + DEPLOYMENT_ID_KEY, deploymentIdFromDeploymentAnnotation);
                });

        AnnotationSupport.findAnnotation(context.getTestMethod(), CmmnDeployment.class)
                .ifPresent(deployment -> {
                    CmmnEngineConfiguration cmmnEngineConfiguration = getCmmnEngineConfiguration(processEngine);
                    String cmmnDeploymentId = CmmnTestHelper.annotationDeploymentSetUp(cmmnEngineConfiguration.getCmmnRepositoryService(),
                            context.getRequiredTestClass(), context.getRequiredTestMethod(), deployment);
                    getStore(context).put(context.getUniqueId() + CMMN_DEPLOYMENT_ID_KEY, cmmnDeploymentId);
                });

    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        ProcessEngine processEngine = getProcessEngine(context);
        String deploymentIdFromAnnotation = getStore(context).remove(context.getUniqueId() + DEPLOYMENT_ID_KEY, String.class);
        if (deploymentIdFromAnnotation != null) {
            TestHelper.annotationDeploymentTearDown(processEngine, deploymentIdFromAnnotation, context.getRequiredTestClass(),
                    context.getRequiredTestMethod().getName());
        }

        String cmmnDeploymentIdFromAnnotation = getStore(context).remove(context.getUniqueId() + CMMN_DEPLOYMENT_ID_KEY, String.class);
        if (cmmnDeploymentIdFromAnnotation != null) {
            CmmnEngineConfiguration cmmnEngineConfiguration = getCmmnEngineConfiguration(processEngine);
            CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, cmmnDeploymentIdFromAnnotation);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        ProcessEngine processEngine = getProcessEngine(context);
        processEngine.getProcessEngineConfiguration().getClock().reset();
        AnnotationSupport.findAnnotation(context.getRequiredTestClass(), EnsureCleanDb.class)
                .ifPresent(ensureCleanDb -> {
                    EnsureCleanDbUtils.assertAndEnsureCleanDb(
                            context.getDisplayName(),
                            LOGGER,
                            processEngine.getProcessEngineConfiguration(),
                            ensureCleanDb,
                            context.getExecutionException().isEmpty(),
                            processEngine.getProcessEngineConfiguration().getSchemaManagementCmd()
                    );
                });
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return ProcessEngine.class.equals(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return getProcessEngine(extensionContext);
    }

    protected ProcessEngine getProcessEngine(ExtensionContext context) {
        String configurationResource = getConfigurationResource(context);
        return getStore(context).getOrComputeIfAbsent(configurationResource, this::initializeProcessEngine, CloseableEngine.class).processEngine;
    }

    protected String getConfigurationResource(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), ConfigurationResource.class)
                .map(ConfigurationResource::value)
                .orElse(FlowableExtension.DEFAULT_CONFIGURATION_RESOURCE);
    }

    protected CloseableEngine initializeProcessEngine(String configurationResource) {
        ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource(configurationResource);
        Map<Object, Object> beans = new HashMap<>();
        processEngineConfiguration.setBeans(beans);
        ProcessEngine processEngine = processEngineConfiguration.buildEngine();
        ProcessEngines.setInitialized(true);
        CmmnEngines.setInitialized(true);
        CmmnEngineConfiguration cmmnEngineConfiguration = getCmmnEngineConfiguration(processEngine);
        beans.put("cmmnRepositoryService", cmmnEngineConfiguration.getCmmnRepositoryService());
        beans.put("cmmnRuntimeService", cmmnEngineConfiguration.getCmmnRuntimeService());
        beans.put("cmmnTaskService", cmmnEngineConfiguration.getCmmnTaskService());
        beans.put("cmmnHistoryService", cmmnEngineConfiguration.getCmmnHistoryService());
        beans.put("cmmnManagementService", cmmnEngineConfiguration.getCmmnManagementService());
        return new CloseableEngine(processEngine);
    }

    protected CmmnEngineConfiguration getCmmnEngineConfiguration(ProcessEngine processEngine) {
        return (CmmnEngineConfiguration) processEngine.getProcessEngineConfiguration()
                .getEngineConfigurations()
                .get(EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG);
    }

    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }

    protected static class CloseableEngine implements AutoCloseable {

        protected final ProcessEngine processEngine;

        public CloseableEngine(ProcessEngine processEngine) {
            this.processEngine = processEngine;
        }

        @Override
        public void close() {
            if (processEngine != null) {
                CmmnEngine cmmnEngine = CmmnEngines.getCmmnEngine(processEngine.getName());
                if (cmmnEngine != null) {
                    cmmnEngine.close();
                }
                processEngine.close();
            }
        }
    }
}
