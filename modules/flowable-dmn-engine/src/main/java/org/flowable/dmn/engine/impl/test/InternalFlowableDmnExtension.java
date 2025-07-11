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
package org.flowable.dmn.engine.impl.test;

import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.test.DmnDeployment;
import org.flowable.dmn.engine.test.DmnDeploymentId;
import org.flowable.dmn.engine.test.DmnTestHelper;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base internal extension for JUnit Jupiter.
 * <ul>
 *     <li>
 *         Performs a deployment before each test when a test method is annotated with {@link DmnDeployment}
 *     </li>
 *     <li>
 *         Assert and ensure a clean db after each test or after all tests (depending on the {@link TestInstance.Lifecycle}.
 *     </li>
 *     <li>
 *         Support for injecting the {@link DmnEngine}, and the id of the deployment done via
 *         {@link DmnDeployment} into test methods and lifecycle methods within tests.
 *     </li>
 * </ul>
 *
 * @author Filip Hrisafov
 */
public abstract class InternalFlowableDmnExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    protected static final String ANNOTATION_DEPLOYMENT_ID_KEY = "deploymentIdFromDeploymentAnnotation";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeEach(ExtensionContext context) {
        DmnEngine dmnEngine = getDmnEngine(context);
        AnnotationSupport.findAnnotation(context.getTestMethod(), DmnDeployment.class)
                .ifPresent(deployment -> {
                    String deploymentId = DmnTestHelper.annotationDeploymentSetUp(dmnEngine, context.getRequiredTestClass(), context.getRequiredTestMethod(),
                            deployment);
                    getStore(context).put(context.getUniqueId() + ANNOTATION_DEPLOYMENT_ID_KEY, deploymentId);
                });

    }

    @Override
    public void afterEach(ExtensionContext context) {
        DmnEngine dmnEngine = getDmnEngine(context);
        dmnEngine.getDmnEngineConfiguration().getClock().reset();
        String deploymentIdFromAnnotation = getStore(context).remove(context.getUniqueId() + ANNOTATION_DEPLOYMENT_ID_KEY, String.class);
        if (deploymentIdFromAnnotation != null) {
            DmnTestHelper.annotationDeploymentTearDown(dmnEngine, deploymentIdFromAnnotation, context.getRequiredTestClass(),
                    context.getRequiredTestMethod().getName());
        }

        AnnotationSupport.findAnnotation(context.getRequiredTestClass(), EnsureCleanDb.class)
                .ifPresent(ensureCleanDb -> {
                    EnsureCleanDbUtils.assertAndEnsureCleanDb(
                            context.getDisplayName(),
                            logger,
                            dmnEngine.getDmnEngineConfiguration(),
                            ensureCleanDb,
                            context.getExecutionException().isEmpty(),
                            dmnEngine.getDmnEngineConfiguration().getSchemaManagementCmd()
                    );
                });

    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return DmnEngine.class.equals(parameterType) || parameterContext.isAnnotated(DmnDeploymentId.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.isAnnotated(DmnDeploymentId.class)) {
            return getStore(extensionContext)
                    .get(extensionContext.getUniqueId() + ANNOTATION_DEPLOYMENT_ID_KEY, String.class);
        }
        return getDmnEngine(extensionContext);
    }

    protected abstract DmnEngine getDmnEngine(ExtensionContext context);

    protected abstract ExtensionContext.Store getStore(ExtensionContext context);
}
