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
package org.flowable.form.engine.impl.test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.test.CleanTest;
import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.flowable.form.engine.test.FormDeploymentAnnotation;
import org.flowable.form.engine.test.FormDeploymentId;
import org.flowable.form.engine.test.FormTestHelper;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base internal extension for JUnit Jupiter. This is a basis for other internal extensions. It allows:
 * <ul>
 * <li>
 * Performs a deployment before each test when a test method is annotated with {@link FormDeploymentAnnotation}
 * </li>
 * <li>
 * Validates the history data after each test
 * </li>
 * <li>
 * Delete history jobs and deployment after each test
 * </li>
 * <li>
 * Assert and ensure a clean db after each test or after all tests (depending on the {@link TestInstance.Lifecycle}.
 * </li>
 * <li>
 * Support for injecting the {@link FormEngine}, {@link FormDeploymentAnnotation} into test methods and lifecycle methods within tests.
 * </li>
 * </ul>
 *
 * @author Filip Hrisafov
 */
public abstract class InternalFlowableFormExtension implements AfterEachCallback, BeforeEachCallback, AfterAllCallback, ParameterResolver {

    protected static final String EMPTY_LINE = "\n";

    protected static final String ANNOTATION_DEPLOYMENT_ID_KEY = "deploymentIdFromFormDeploymentAnnotation";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeEach(ExtensionContext context) {
        FormEngine formEngine = getFormEngine(context);

        AnnotationSupport.findAnnotation(context.getTestMethod(), FormDeploymentAnnotation.class)
            .ifPresent(deployment -> {
                String deploymentIdFromDeploymentAnnotation = FormTestHelper
                    .annotationDeploymentSetUp(formEngine, context.getRequiredTestClass(), context.getRequiredTestMethod(), deployment);
                getStore(context).put(context.getUniqueId() + ANNOTATION_DEPLOYMENT_ID_KEY, deploymentIdFromDeploymentAnnotation);
            });
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        doFinally(context, TestInstance.Lifecycle.PER_METHOD);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        doFinally(context, TestInstance.Lifecycle.PER_CLASS);
    }

    protected void doFinally(ExtensionContext context, TestInstance.Lifecycle lifecycleForClean) {
        FormEngine formEngine = getFormEngine(context);
        FormEngineConfiguration formEngineConfiguration = formEngine.getFormEngineConfiguration();
        try {
            String annotationDeploymentKey = context.getUniqueId() + ANNOTATION_DEPLOYMENT_ID_KEY;
            String deploymentIdFromDeploymentAnnotation = getStore(context).get(annotationDeploymentKey, String.class);
            if (deploymentIdFromDeploymentAnnotation != null) {
                FormTestHelper.annotationDeploymentTearDown(formEngine, deploymentIdFromDeploymentAnnotation, context.getRequiredTestClass(),
                    context.getRequiredTestMethod().getName());
                getStore(context).remove(annotationDeploymentKey);
            }

            AnnotationSupport.findAnnotation(context.getTestMethod(), CleanTest.class)
                .ifPresent(cleanTest -> removeDeployments(formEngine.getFormRepositoryService()));
            if (context.getTestInstanceLifecycle().orElse(TestInstance.Lifecycle.PER_METHOD) == lifecycleForClean) {
                cleanTestAndAssertAndEnsureCleanDb(context, formEngine);
            }

        } finally {
            formEngineConfiguration.getClock().reset();
        }
    }

    protected void cleanTestAndAssertAndEnsureCleanDb(ExtensionContext context, FormEngine formEngine) {
        AnnotationSupport.findAnnotation(context.getRequiredTestClass(), CleanTest.class)
            .ifPresent(cleanTest -> removeDeployments(getFormEngine(context).getFormRepositoryService()));
        AnnotationSupport.findAnnotation(context.getRequiredTestClass(), EnsureCleanDb.class)
            .ifPresent(ensureCleanDb -> assertAndEnsureCleanDb(formEngine, context, ensureCleanDb));
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    protected void assertAndEnsureCleanDb(FormEngine formEngine, ExtensionContext context, EnsureCleanDb ensureCleanDb) {
        logger.debug("verifying that db is clean after test");
        Set<String> tableNamesExcludedFromDbCleanCheck = new HashSet<>(Arrays.asList(ensureCleanDb.excludeTables()));
        FormManagementService managementService = formEngine.getFormManagementService();
        FormEngineConfiguration formEngineConfiguration = formEngine.getFormEngineConfiguration();
        Map<String, Long> tableCounts = managementService.getTableCount();
        StringBuilder outputMessage = new StringBuilder();
        for (String tableName : tableCounts.keySet()) {
            String tableNameWithoutPrefix = tableName.replace(formEngineConfiguration.getDatabaseTablePrefix(), "");
            if (!tableNamesExcludedFromDbCleanCheck.contains(tableNameWithoutPrefix)) {
                Long count = tableCounts.get(tableName);
                if (count != 0L) {
                    outputMessage.append("  ").append(tableName).append(": ").append(count).append(" record(s) ");
                }
            }
        }
        if (outputMessage.length() > 0) {
            outputMessage.insert(0, "DB NOT CLEAN: \n");
            logger.error(EMPTY_LINE);
            logger.error(outputMessage.toString());

            logger.info("dropping and recreating db");

            if (ensureCleanDb.dropDb()) {
                CommandExecutor commandExecutor = formEngineConfiguration.getCommandExecutor();
                CommandConfig config = new CommandConfig().transactionNotSupported();
                commandExecutor.execute(config, new Command<Object>() {

                    @Override
                    public Object execute(CommandContext commandContext) {
                        SchemaManager dbSchemaManager = CommandContextUtil.getFormEngineConfiguration(commandContext).getDbSchemaManager();
                        dbSchemaManager.schemaDrop();
                        dbSchemaManager.schemaCreate();
                        return null;
                    }
                });
            }

            if (!context.getExecutionException().isPresent()) {
                throw new AssertionError(outputMessage.toString());
            }
        } else {
            logger.info("database was clean");
        }
    }

    protected void removeDeployments(FormRepositoryService repositoryService) {
        for (FormDeployment deployment : repositoryService.createDeploymentQuery().list()) {
            try {
                repositoryService.deleteDeployment(deployment.getId());
            } catch (FlowableOptimisticLockingException flowableOptimisticLockingException) {
                logger.warn("Caught exception, retrying", flowableOptimisticLockingException);
                repositoryService.deleteDeployment(deployment.getId());
            }
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return FormEngine.class.equals(parameterType) || parameterContext.isAnnotated(FormDeploymentId.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        if (parameterContext.isAnnotated(FormDeploymentId.class)) {
            return getStore(context).get(context.getUniqueId() + ANNOTATION_DEPLOYMENT_ID_KEY, String.class);
        }
        return getFormEngine(context);
    }

    protected abstract FormEngine getFormEngine(ExtensionContext context);

    protected abstract ExtensionContext.Store getStore(ExtensionContext context);
}
