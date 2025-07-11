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
package org.flowable.app.engine.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.flowable.app.api.AppManagementService;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.engine.AppEngine;
import org.flowable.app.engine.AppEngineConfiguration;
import org.flowable.app.engine.test.impl.AppTestHelper;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * JUnit Jupiter extension for the Flowable AppEngine and services initialization.
 *
 * <p>
 * Usage:
 * </p>
 *
 * <pre>
 * &#64;ExtendWith(FlowableAppExtension.class)
 * class YourTest {
 *
 *   &#64;BeforeEach
 *   void setUp(AppEngine appEngine) {
 *       ...
 *   }
 *
 *   &#64;Test
 *   void myTest(AppRepositoryService appRepositoryService) {
 *       ...
 *   }
 *
 *   ...
 * }
 * </pre>
 *
 * <p>
 * The AppEngine and the services will be made available to the test class through the parameter resolution (BeforeEach, AfterEach, test methods).
 * The AppEngine will be initialized by default with the flowable.app.cfg.xml resource on the classpath.
 * To specify a different configuration file, annotate your class with {@link AppConfigurationResource}.
 * App engines will be cached as part of the JUnit Jupiter Extension context.
 * Right before the first time the setUp is called for a given configuration resource, the cmmn engine will be constructed.
 * </p>
 *
 * <p>
 * You can declare a deployment with the {@link AppDeployment} annotation. This extension will make sure that this deployment gets deployed before the setUp
 * and {@link AppRepositoryService#deleteDeployment(String, boolean) cascade deleted} after the tearDown.
 * The id of the deployment can be accessed by using {@link AppDeploymentId} in a test method.
 * </p>
 *
 * @author Filip Hrisafov
 */
public class FlowableAppExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

    public static final String DEFAULT_CONFIGURATION_RESOURCE = "flowable.app.cfg.xml";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FlowableAppExtension.class);

    private static final Set<Class<?>> SUPPORTED_PARAMETER_TYPES = Set.of(
            AppEngine.class,
            AppEngineConfiguration.class,
            AppRepositoryService.class,
            AppManagementService.class
    );

    @Override
    public void beforeEach(ExtensionContext context) {
        FlowableAppTestHelper testHelper = getTestHelper(context);
        AnnotationSupport.findAnnotation(context.getTestMethod(), AppDeployment.class)
                .ifPresent(deployment -> {
                    String deploymentId = AppTestHelper.annotationDeploymentSetUp(testHelper.getAppEngine(), context.getRequiredTestClass(),
                            context.getRequiredTestMethod(), deployment);
                    testHelper.setDeploymentIdFromDeploymentAnnotation(deploymentId);
                });

    }

    @Override
    public void afterEach(ExtensionContext context) {
        FlowableAppTestHelper testHelper = getTestHelper(context);
        String deploymentId = testHelper.getDeploymentIdFromDeploymentAnnotation();
        if (deploymentId != null) {
            testHelper.getAppEngine().getAppRepositoryService().deleteDeployment(deploymentId, true);
            testHelper.setDeploymentIdFromDeploymentAnnotation(null);
        }
        testHelper.getAppEngine().getAppEngineConfiguration().getClock().reset();

    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return SUPPORTED_PARAMETER_TYPES.contains(parameterType)
                || AnnotationSupport.isAnnotated(parameterContext.getParameter(), AppDeploymentId.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        FlowableAppTestHelper testHelper = getTestHelper(extensionContext);
        if (parameterContext.isAnnotated(AppDeploymentId.class)) {
            return testHelper.getDeploymentIdFromDeploymentAnnotation();
        }

        Class<?> parameterType = parameterContext.getParameter().getType();
        AppEngine appEngine = testHelper.getAppEngine();
        if (parameterType.isInstance(appEngine)) {
            return appEngine;
        }

        try {
            return AppEngine.class.getDeclaredMethod("get" + parameterType.getSimpleName()).invoke(appEngine);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new ParameterResolutionException("Could not find service " + parameterType, e);
        }
    }

    protected String getConfigurationResource(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), AppConfigurationResource.class)
                .map(AppConfigurationResource::value)
                .orElse(DEFAULT_CONFIGURATION_RESOURCE);
    }

    protected FlowableAppTestHelper getTestHelper(ExtensionContext context) {
        return getStore(context)
                .getOrComputeIfAbsent(context.getRequiredTestClass(), key -> new FlowableAppTestHelper(createAppEngine(context)), FlowableAppTestHelper.class);
    }

    protected AppEngine createAppEngine(ExtensionContext context) {
        return AppTestHelper.getAppEngine(getConfigurationResource(context));
    }

    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
