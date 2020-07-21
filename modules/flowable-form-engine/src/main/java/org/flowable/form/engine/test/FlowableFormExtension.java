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
package org.flowable.form.engine.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.flowable.form.api.FormManagementService;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
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
 * JUnit Jupiter extension for the Flowable FormEngine and services initialization.
 *
 * <p>
 * Usage:
 * </p>
 *
 * <pre>
 * &#64;ExtendWith(FlowableFormExtension.class)
 * class YourTest {
 *
 *   &#64;BeforeEach
 *   void setUp(FormEngine FormEngine) {
 *       ...
 *   }
 *
 *   &#64;Test
 *   void myTest(FormRepositoryService formRepositoryService) {
 *       ...
 *   }
 *
 *   ...
 * }
 * </pre>
 *
 * <p>
 * The FormEngine and the services will be made available to the test class through the parameter resolution (BeforeEach, AfterEach, test methods).
 * The FormEngine will be initialized by default with the flowable.form.cfg.xml resource on the classpath.
 * To specify a different configuration file, annotate your class with {@link FormConfigurationResource}.
 * Form engines will be cached as part of the JUnit Jupiter Extension context.
 * Right before the first time the setUp is called for a given configuration resource, the form engine will be constructed.
 * </p>
 *
 * <p>
 * You can declare a deployment with the {@link FormDeploymentAnnotation} annotation. This extension will make sure that this deployment gets deployed
 * before the setUp and {@link FormRepositoryService#deleteDeployment(String, boolean)}  deleted} after the tearDown.
 * The id of the deployment can be accessed by using {@link FormDeploymentId} in a test method.
 * </p>
 *
 * <p>
 * {@link FlowableFormTestHelper#setCurrentTime(Date)}  can be used to set the current time used by the form engine}
 * This can be handy to control the exact time that is used by the engine in order to verify e.g. e.g. due dates of timers.
 * Or start, end and duration times in the history service. In the tearDown, the internal clock will automatically be reset to use the current system
 * time rather then the time that was set during a test method.
 * </p>
 *
 * @author Filip Hrisafov
 */
public class FlowableFormExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

    public static final String DEFAULT_CONFIGURATION_RESOURCE = "flowable.form.cfg.xml";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FlowableFormExtension.class);

    private static final Set<Class<?>> SUPPORTED_PARAMETERS = new HashSet<>(Arrays.asList(
        FormEngineConfiguration.class,
        FormEngine.class,
        FormRepositoryService.class,
        FormManagementService.class,
        FormService.class
    ));

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final String configurationResource;

    public FlowableFormExtension() {
        this("flowable.form.cfg.xml");
    }

    public FlowableFormExtension(String configurationResource) {
        this.configurationResource = configurationResource;
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        AnnotationSupport.findAnnotation(context.getTestMethod(), FormDeploymentAnnotation.class)
            .ifPresent(deployment -> {
                FlowableFormTestHelper testHelper = getTestHelper(context);
                String deploymentIdFromDeploymentAnnotation = FormTestHelper
                    .annotationDeploymentSetUp(testHelper.getFormEngine(), context.getRequiredTestClass(), context.getRequiredTestMethod(),
                        deployment);
                testHelper.setDeploymentIdFromDeploymentAnnotation(deploymentIdFromDeploymentAnnotation);
            });

    }

    @Override
    public void afterEach(ExtensionContext context) {
        FlowableFormTestHelper flowableTestHelper = getTestHelper(context);
        FormEngine formEngine = flowableTestHelper.getFormEngine();
        String deploymentIdFromDeploymentAnnotation = flowableTestHelper.getDeploymentIdFromDeploymentAnnotation();
        if (deploymentIdFromDeploymentAnnotation != null) {
            FormTestHelper.annotationDeploymentTearDown(formEngine, deploymentIdFromDeploymentAnnotation, context.getRequiredTestClass(),
                context.getRequiredTestMethod().getName());
            flowableTestHelper.setDeploymentIdFromDeploymentAnnotation(null);
        }

        formEngine.getFormEngineConfiguration().getClock().reset();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return SUPPORTED_PARAMETERS.contains(parameterType) || FlowableFormTestHelper.class.equals(parameterType)
            || parameterContext.isAnnotated(FormDeploymentId.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        FlowableFormTestHelper flowableTestHelper = getTestHelper(context);
        if (parameterContext.isAnnotated(FormDeploymentId.class)) {
            return flowableTestHelper.getDeploymentIdFromDeploymentAnnotation();
        }

        Class<?> parameterType = parameterContext.getParameter().getType();
        FormEngine formEngine = flowableTestHelper.getFormEngine();
        if (parameterType.isInstance(formEngine)) {
            return formEngine;
        } else if (FlowableFormTestHelper.class.equals(parameterType)) {
            return flowableTestHelper;
        }

        try {
            return FormEngine.class.getDeclaredMethod("get" + parameterType.getSimpleName()).invoke(formEngine);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new ParameterResolutionException("Could not find service " + parameterType, ex);
        }
    }

    protected String getConfigurationResource(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), FormConfigurationResource.class)
            .map(FormConfigurationResource::value)
            .orElse(DEFAULT_CONFIGURATION_RESOURCE);
    }

    protected FlowableFormTestHelper getTestHelper(ExtensionContext context) {
        return getStore(context)
            .getOrComputeIfAbsent(context.getRequiredTestClass(), key -> new FlowableFormTestHelper(createFormEngine(context)), FlowableFormTestHelper.class);
    }

    protected FormEngine createFormEngine(ExtensionContext context) {
        return FormTestHelper.getFormEngine(getConfigurationResource(context));
    }

    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
