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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CmmnDeploymentBuilder;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeployer;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class CmmnTestRunner extends BlockJUnit4ClassRunner {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CmmnTestRunner.class);
    
    protected static CmmnEngineConfiguration cmmnEngineConfiguration;
    protected static String deploymentId;

    protected static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = Arrays.asList(
            "ACT_GE_PROPERTY",
            "ACT_ID_PROPERTY",
            "ACT_CMMN_DATABASECHANGELOG",
            "ACT_CMMN_DATABASECHANGELOGLOCK",
            "ACT_FO_DATABASECHANGELOG",
            "ACT_FO_DATABASECHANGELOGLOCK",
            "FLW_EV_DATABASECHANGELOG",
            "FLW_EV_DATABASECHANGELOGLOCK"
    );


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
        try {
            super.runChild(method, notifier);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        if (method.getAnnotation(Ignore.class) == null && method.getAnnotation(CmmnDeployment.class) != null) {

            List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(Before.class);

            return new Statement() {

                @Override
                public void evaluate() throws Throwable {
                    for (FrameworkMethod before : befores) {
                        before.invokeExplosively(target);
                    }
                    deploymentId = deployCmmnDefinition(method);
                    statement.evaluate();
                }

            };
        } else {
            return super.withBefores(method, target, statement);
        }

    }

    @Override
    protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
        List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(After.class);

        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList<>();
                try {
                    statement.evaluate();
                } catch (Throwable e) {
                    errors.add(e);
                } finally {

                    if (deploymentId != null) {
                        CmmnTestHelper.deleteDeployment(cmmnEngineConfiguration, deploymentId);
                        deploymentId = null;
                    }

                    for (FrameworkMethod each : afters) {
                        try {
                            each.invokeExplosively(target);
                        } catch (Throwable e) {
                            errors.add(e);
                        }
                    }

                    if (errors.isEmpty()) {
                        assertDatabaseEmpty(method);
                    }

                }
                MultipleFailureException.assertEmpty(errors);
            }

        };
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

            String[] extraResources = deploymentAnnotation.extraResources();
            if (extraResources != null && extraResources.length > 0) {
                for (String extraResource : extraResources) {
                    deploymentBuilder.addClasspathResource(extraResource);
                }
            }
            
            if (StringUtils.isNotEmpty(deploymentAnnotation.tenantId())) {
                deploymentBuilder.tenantId(deploymentAnnotation.tenantId());
            }
            
            return deploymentBuilder.deploy().getId();
            
        } catch (Exception e) {
            throw new FlowableException("Error while deploying case definition", e);
        }
    }
    
    protected String getCmmnDefinitionResource(FrameworkMethod method) {
        String className = method.getMethod().getDeclaringClass().getName().replace('.', '/');
        String methodName = method.getName();
        for (String suffix : CmmnDeployer.CMMN_RESOURCE_SUFFIXES) {
            String resource = className + "." + methodName + suffix;
            if (CmmnTestRunner.class.getClassLoader().getResource(resource) != null) {
                return resource;
            }
        }
        return className + "." + method.getName() + ".cmmn";
    }

    protected void assertDatabaseEmpty(FrameworkMethod method) {
        EnsureCleanDbUtils.assertAndEnsureCleanDb(
            getTestClass().getName() + "." + method.getName(),
            LOGGER,
            cmmnEngineConfiguration,
            TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK,
            true,
            new Command<Void>() {

                @Override
                public Void execute(CommandContext commandContext) {
                    SchemaManager schemaManager = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getSchemaManager();
                    schemaManager.schemaDrop();
                    schemaManager.schemaCreate();
                    return null;
                }
            }

        );
    }

}
