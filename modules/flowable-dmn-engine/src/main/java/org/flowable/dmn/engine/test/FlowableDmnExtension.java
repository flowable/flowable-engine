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
package org.flowable.dmn.engine.test;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
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
 * JUnit Jupiter extension for the Flowable DmnEngine and services initialization.
 *
 * <p>
 * Usage:
 * </p>
 *
 * <pre>
 * &#64;ExtendWith(FlowableDmnExtension.class)
 * class YourTest {
 *
 *   &#64;BeforeEach
 *   void setUp(DmnEngine dmnEngine) {
 *       ...
 *   }
 *
 *   &#64;Test
 *   void myTest(DmnRuleService ruleService) {
 *       ...
 *   }
 *
 *   ...
 * }
 * </pre>
 *
 * <p>
 * The DmnEngine and the services will be made available to the test class through the parameter resolution (BeforeEach, AfterEach, test methods).
 * The DmnEngine will be initialized by default with the flowable.dmn.cfg.xml resource on the classpath.
 * To specify a different configuration file, annotate your class with {@link DmnConfigurationResource}.
 * Dmn engines will be cached as part of the JUnit Jupiter Extension context.
 * Right before the first time the setUp is called for a given configuration resource, the dmn engine will be constructed.
 * </p>
 *
 * <p>
 * You can declare a deployment with the {@link DmnDeployment} or {@link DmnDeploymentAnnotation} annotation.
 * If both annotations are used then {@link DmnDeployment} takes precedence and {@link DmnDeploymentAnnotation} will be ignored.
 * This extensions will make sure that this deployment gets deployed before the setUp
 * and {@link org.flowable.dmn.api.DmnRepositoryService#deleteDeployment(String)} cascade deleted} after the tearDown.
 * The id of the deployment can be accessed by using {@link DmnDeploymentId} in a test or lifecycle method.
 * </p>
 *
 * <p>
 * {@link FlowableDmnTestHelper#setCurrentTime(Instant)} can be used to set the current time used by the dmn engine}
 * This can be handy to control the exact time that is used by the engine in order to verify e.g. due dates.
 * Or start, end and duration times in the history service. In the tearDown, the internal clock will automatically be reset to use the current system
 * time rather then the time that was set during a test method.
 * </p>
 *
 * @author Filip Hrisafov
 */
public class FlowableDmnExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

    public static final String DEFAULT_CONFIGURATION_RESOURCE = "flowable.dmn.cfg.xml";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FlowableDmnExtension.class);

    private static final Set<Class<?>> SUPPORTED_PARAMETERS = new HashSet<>(Arrays.asList(
        DmnEngineConfiguration.class,
        DmnEngine.class,
        DmnRepositoryService.class,
        DmnDecisionService.class,
        DmnHistoryService.class,
        DmnManagementService.class
    ));

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeEach(ExtensionContext context) {
        FlowableDmnTestHelper flowableTestHelper = getTestHelper(context);

        AnnotationSupport.findAnnotation(context.getTestMethod(), DmnDeployment.class)
            .ifPresent(deployment -> {
                String deploymentIdFromDeploymentAnnotation = DmnTestHelper
                    .annotationDeploymentSetUp(flowableTestHelper.getDmnEngine(), context.getRequiredTestClass(), context.getRequiredTestMethod(),
                        deployment);
                flowableTestHelper.setDeploymentIdFromDeploymentAnnotation(deploymentIdFromDeploymentAnnotation);
            });

        if (flowableTestHelper.getDeploymentIdFromDeploymentAnnotation() == null) {
            // If @DmnDeployment was used already then don't look for @DmnDeploymentAnnotation
            AnnotationSupport.findAnnotation(context.getTestMethod(), DmnDeploymentAnnotation.class)
                .ifPresent(deployment -> {
                    String deploymentIdFromDeploymentAnnotation = DmnTestHelper
                        .annotationDeploymentSetUp(flowableTestHelper.getDmnEngine(), context.getRequiredTestClass(), context.getRequiredTestMethod(),
                            deployment);
                    flowableTestHelper.setDeploymentIdFromDeploymentAnnotation(deploymentIdFromDeploymentAnnotation);
                });
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        FlowableDmnTestHelper flowableTestHelper = getTestHelper(context);
        DmnEngine dmnEngine = flowableTestHelper.getDmnEngine();
        String deploymentIdFromDeploymentAnnotation = flowableTestHelper.getDeploymentIdFromDeploymentAnnotation();
        if (deploymentIdFromDeploymentAnnotation != null) {
            DmnTestHelper.annotationDeploymentTearDown(dmnEngine, deploymentIdFromDeploymentAnnotation, context.getRequiredTestClass(),
                context.getRequiredTestMethod().getName());
            flowableTestHelper.setDeploymentIdFromDeploymentAnnotation(null);
        }

        dmnEngine.getDmnEngineConfiguration().getClock().reset();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return SUPPORTED_PARAMETERS.contains(parameterType) || FlowableDmnTestHelper.class.equals(parameterType)
            || parameterContext.isAnnotated(DmnDeploymentId.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        FlowableDmnTestHelper flowableTestHelper = getTestHelper(context);
        if (parameterContext.isAnnotated(DmnDeploymentId.class)) {
            return flowableTestHelper.getDeploymentIdFromDeploymentAnnotation();
        }

        Class<?> parameterType = parameterContext.getParameter().getType();
        DmnEngine dmnEngine = flowableTestHelper.getDmnEngine();
        if (parameterType.isInstance(dmnEngine)) {
            return dmnEngine;
        } else if (FlowableDmnTestHelper.class.equals(parameterType)) {
            return flowableTestHelper;
        }

        try {
            return DmnEngine.class.getDeclaredMethod("get" + parameterType.getSimpleName()).invoke(dmnEngine);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new ParameterResolutionException("Could not find service " + parameterType, ex);
        }
    }

    protected String getConfigurationResource(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), DmnConfigurationResource.class)
            .map(DmnConfigurationResource::value)
            .orElse(DEFAULT_CONFIGURATION_RESOURCE);
    }

    protected FlowableDmnTestHelper getTestHelper(ExtensionContext context) {
        return getStore(context)
            .getOrComputeIfAbsent(context.getRequiredTestClass(),
                key -> new FlowableDmnTestHelper(createDmnEngine(context)), FlowableDmnTestHelper.class);
    }

    protected DmnEngine createDmnEngine(ExtensionContext context) {
        return DmnTestHelper.getDmnEngine(getConfigurationResource(context));
    }

    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
