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
package org.flowable.cmmn.engine.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.impl.CmmnTestHelper;
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
 * JUnit Jupiter extension for the Flowable CmmnEngine and services initialization.
 *
 * <p>
 * Usage:
 * </p>
 *
 * <pre>
 * &#64;ExtendWith(FlowableCmmnExtension.class)
 * class YourTest {
 *
 *   &#64;BeforeEach
 *   void setUp(CmmnEngine cmmnEngine) {
 *       ...
 *   }
 *
 *   &#64;Test
 *   void myTest(CmmnRuntimeService cmmnRuntimeService) {
 *       ...
 *   }
 *
 *   ...
 * }
 * </pre>
 *
 * <p>
 * The CmmnEngine and the services will be made available to the test class through the parameter resolution (BeforeEach, AfterEach, test methods).
 * The CmmnEngine will be initialized by default with the flowable.cmmn.cfg.xml resource on the classpath.
 * To specify a different configuration file, annotate your class with {@link CmmnConfigurationResource}.
 * Cmmn engines will be cached as part of the JUnit Jupiter Extension context.
 * Right before the first time the setUp is called for a given configuration resource, the cmmn engine will be constructed.
 * </p>
 *
 * <p>
 * You can declare a deployment with the {@link CmmnDeployment} annotation. This extension will make sure that this deployment gets deployed before the setUp
 * and {@link CmmnRepositoryService#deleteDeployment(String, boolean) cascade deleted} after the tearDown.
 * The id of the deployment can be accessed by using {@link CmmnDeploymentId} in a test method.
 * </p>
 *
 * <p>
 * {@link FlowableCmmnTestHelper#setCurrentTime(java.time.Instant) can be used to set the current time used by the cmmn engine}
 * This can be handy to control the exact time that is used by the engine in order to verify e.g. e.g. due dates of timers.
 * Or start, end and duration times in the history service. In the tearDown, the internal clock will automatically be reset to use the current system
 * time rather then the time that was set during a test method.
 * </p>
 *
 * @author Filip Hrisafov
 */
public class FlowableCmmnExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

    public static final String DEFAULT_CONFIGURATION_RESOURCE = "flowable.cmmn.cfg.xml";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FlowableCmmnExtension.class);

    private static final Set<Class<?>> SUPPORTED_PARAMETERS = new HashSet<>(Arrays.asList(
        CmmnEngineConfiguration.class,
        CmmnEngine.class,
        CmmnRepositoryService.class,
        CmmnRuntimeService.class,
        CmmnTaskService.class,
        CmmnHistoryService.class,
        CmmnManagementService.class
    ));

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeEach(ExtensionContext context) {
        FlowableCmmnTestHelper flowableTestHelper = getTestHelper(context);

        AnnotationSupport.findAnnotation(context.getTestMethod(), CmmnDeployment.class)
            .ifPresent(deployment -> {
                String deploymentIdFromDeploymentAnnotation = CmmnTestHelper
                    .annotationDeploymentSetUp(flowableTestHelper.getCmmnEngine(), context.getRequiredTestClass(), context.getRequiredTestMethod(),
                        deployment);
                flowableTestHelper.setDeploymentIdFromDeploymentAnnotation(deploymentIdFromDeploymentAnnotation);
            });

    }

    @Override
    public void afterEach(ExtensionContext context) {
        FlowableCmmnTestHelper flowableTestHelper = getTestHelper(context);
        CmmnEngine cmmnEngine = flowableTestHelper.getCmmnEngine();
        String deploymentIdFromDeploymentAnnotation = flowableTestHelper.getDeploymentIdFromDeploymentAnnotation();
        if (deploymentIdFromDeploymentAnnotation != null) {
            CmmnTestHelper.annotationDeploymentTearDown(cmmnEngine, deploymentIdFromDeploymentAnnotation, context.getRequiredTestClass(),
                context.getRequiredTestMethod().getName());
            flowableTestHelper.setDeploymentIdFromDeploymentAnnotation(null);
        }

        cmmnEngine.getCmmnEngineConfiguration().getClock().reset();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return SUPPORTED_PARAMETERS.contains(parameterType) || FlowableCmmnTestHelper.class.equals(parameterType)
            || parameterContext.isAnnotated(CmmnDeploymentId.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        FlowableCmmnTestHelper flowableTestHelper = getTestHelper(context);
        if (parameterContext.isAnnotated(CmmnDeploymentId.class)) {
            return flowableTestHelper.getDeploymentIdFromDeploymentAnnotation();
        }

        Class<?> parameterType = parameterContext.getParameter().getType();
        CmmnEngine cmmnEngine = flowableTestHelper.getCmmnEngine();
        if (parameterType.isInstance(cmmnEngine)) {
            return cmmnEngine;
        } else if (FlowableCmmnTestHelper.class.equals(parameterType)) {
            return flowableTestHelper;
        }

        try {
            return CmmnEngine.class.getDeclaredMethod("get" + parameterType.getSimpleName()).invoke(cmmnEngine);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new ParameterResolutionException("Could not find service " + parameterType, ex);
        }
    }

    protected String getConfigurationResource(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), CmmnConfigurationResource.class)
            .map(CmmnConfigurationResource::value)
            .orElse(DEFAULT_CONFIGURATION_RESOURCE);
    }

    protected FlowableCmmnTestHelper getTestHelper(ExtensionContext context) {
        return getStore(context)
            .getOrComputeIfAbsent(context.getRequiredTestClass(),
                key -> new FlowableCmmnTestHelper(createCmmnEngine(context)), FlowableCmmnTestHelper.class);
    }

    protected CmmnEngine createCmmnEngine(ExtensionContext context) {
        return CmmnTestHelper.getCmmnEngine(getConfigurationResource(context));
    }

    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
