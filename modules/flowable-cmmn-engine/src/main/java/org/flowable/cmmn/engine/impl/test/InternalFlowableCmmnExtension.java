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
package org.flowable.cmmn.engine.impl.test;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.CmmnDeploymentId;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.common.engine.impl.test.EnsureCleanDbUtils;
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
public abstract class InternalFlowableCmmnExtension implements AfterTestExecutionCallback, AfterEachCallback, BeforeEachCallback, ParameterResolver {

    protected static final String ANNOTATION_DEPLOYMENT_ID_KEY = "deploymentIdFromDeploymentAnnotation";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeEach(ExtensionContext context) {
        CmmnEngine cmmnEngine = getCmmnEngine(context);
        AnnotationSupport.findAnnotation(context.getTestMethod(), CmmnDeployment.class)
                .ifPresent(deployment -> {
                    String deploymentId = CmmnTestHelper.annotationDeploymentSetUp(cmmnEngine, context.getRequiredTestClass(), context.getRequiredTestMethod(),
                            deployment);
                    getStore(context).put(context.getUniqueId() + ANNOTATION_DEPLOYMENT_ID_KEY, deploymentId);
                });

    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
        CmmnEngine cmmnEngine = getCmmnEngine(context);
        cmmnEngine.getCmmnEngineConfiguration().getClock().reset();
        Authentication.setAuthenticatedUserId(null);

        String deploymentIdFromAnnotation = getStore(context).remove(context.getUniqueId() + ANNOTATION_DEPLOYMENT_ID_KEY, String.class);
        if (deploymentIdFromAnnotation != null) {
            CmmnTestHelper.annotationDeploymentTearDown(cmmnEngine, deploymentIdFromAnnotation, context.getRequiredTestClass(),
                    context.getRequiredTestMethod().getName());
        }

    }

    @Override
    public void afterEach(ExtensionContext context) {
        CmmnEngine cmmnEngine = getCmmnEngine(context);
        AnnotationSupport.findAnnotation(context.getRequiredTestClass(), EnsureCleanDb.class)
                .ifPresent(ensureCleanDb ->
                        EnsureCleanDbUtils.assertAndEnsureCleanDb(
                                context.getDisplayName(),
                                logger,
                                cmmnEngine.getCmmnEngineConfiguration(),
                                ensureCleanDb,
                                context.getExecutionException().isEmpty(),
                                cmmnEngine.getCmmnEngineConfiguration().getSchemaManagementCmd()
                        ));
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return CmmnEngine.class.equals(parameterType) || parameterContext.isAnnotated(CmmnDeploymentId.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        if (parameterContext.isAnnotated(CmmnDeploymentId.class)) {
            return getStore(extensionContext)
                    .get(extensionContext.getUniqueId() + ANNOTATION_DEPLOYMENT_ID_KEY, String.class);
        }
        return getCmmnEngine(extensionContext);
    }

    protected abstract CmmnEngine getCmmnEngine(ExtensionContext context);

    protected abstract ExtensionContext.Store getStore(ExtensionContext context);
}
