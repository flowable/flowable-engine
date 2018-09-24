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
package org.flowable.engine.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.test.TestHelper;
import org.flowable.engine.test.mock.FlowableMockSupport;
import org.flowable.engine.test.mock.MockServiceTask;
import org.flowable.engine.test.mock.NoOpServiceTasks;
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
 * JUnit Jupiter extension for the Flowable ProcessEngine and services initialization.
 *
 * <p>
 * Usage:
 * </p>
 *
 * <pre>
 * &#64;FlowableTest
 * class YourTest {
 *
 *   &#64;BeforeEach
 *   void setUp(ProcessEngine processEngine) {
 *       ...
 *   }
 *
 *   &#64;Test
 *   void myTest(RuntimeService runtimeService) {
 *       ...
 *   }
 *
 *   ...
 * }
 * </pre>
 *
 * <p>
 * The ProcessEngine and the services will be made available to the test class through the parameter resolution (BeforeEach, AfterEach, test methods).
 * The ProcessEngine will be initialized by default with the flowable.cfg.xml resource on the classpath.
 * To specify a different configuration file, annotate your class witn {@link ConfigurationResource}.
 * Process engines will be cached as part of the JUnit Jupiter Extension context.
 * Right before the first time the setUp is called for a given configuration resource, the process engine will be constructed.
 * </p>
 *
 * <p>
 * You can declare a deployment with the {@link Deployment} annotation. This extension will make sure that this deployment gets deployed before the setUp and
 * {@link RepositoryService#deleteDeployment(String, boolean) cascade deleted} after the tearDown.
 * The id of the deployment can be accessed by using {@link DeploymentId} in a test method.
 * </p>
 *
 * <p>
 * {@link FlowableTestHelper#setCurrentTime(Date) can be used to set the current time used by the process engine}
 * This can be handy to control the exact time that is used by the engine in order to verify e.g. e.g. due dates of timers.
 * Or start, end and duration times in the history service. In the tearDown, the internal clock will automatically be reset to use the current system
 * time rather then the time that was set during a test method.
 * </p>
 *
 * @author Filip Hrisafov
 */
public class FlowableExtension implements ParameterResolver, BeforeEachCallback, AfterEachCallback {

    public static final String DEFAULT_CONFIGURATION_RESOURCE = "flowable.cfg.xml";

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(FlowableExtension.class);

    private static final Set<Class<?>> SUPPORTED_PARAMETERS = new HashSet<>(Arrays.asList(
        ProcessEngineConfiguration.class,
        ProcessEngine.class,
        RepositoryService.class,
        RuntimeService.class,
        TaskService.class,
        HistoryService.class,
        IdentityService.class,
        ManagementService.class,
        FormService.class
    ));

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        FlowableTestHelper flowableTestHelper = getTestHelper(context);
        FlowableMockSupport mockSupport = flowableTestHelper.getMockSupport();

        if (mockSupport != null) {
            AnnotationSupport.findRepeatableAnnotations(context.getRequiredTestClass(), MockServiceTask.class)
                .forEach(mockServiceTask -> TestHelper.handleMockServiceTaskAnnotation(mockSupport, mockServiceTask));
            AnnotationSupport.findRepeatableAnnotations(context.getRequiredTestMethod(), MockServiceTask.class)
                .forEach(mockServiceTask -> TestHelper.handleMockServiceTaskAnnotation(mockSupport, mockServiceTask));
            AnnotationSupport.findAnnotation(context.getRequiredTestMethod(), NoOpServiceTasks.class)
                .ifPresent(noOpServiceTasks -> TestHelper.handleNoOpServiceTasksAnnotation(mockSupport, noOpServiceTasks));
        }

        AnnotationSupport.findAnnotation(context.getTestMethod(), Deployment.class)
            .ifPresent(deployment -> {
                String deploymentIdFromDeploymentAnnotation = TestHelper
                    .annotationDeploymentSetUp(flowableTestHelper.getProcessEngine(), context.getRequiredTestClass(), context.getRequiredTestMethod(),
                        deployment);
                flowableTestHelper.setDeploymentIdFromDeploymentAnnotation(deploymentIdFromDeploymentAnnotation);
            });

    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        FlowableTestHelper flowableTestHelper = getTestHelper(context);
        ProcessEngine processEngine = flowableTestHelper.getProcessEngine();
        String deploymentIdFromDeploymentAnnotation = flowableTestHelper.getDeploymentIdFromDeploymentAnnotation();
        if (deploymentIdFromDeploymentAnnotation != null) {
            TestHelper.annotationDeploymentTearDown(processEngine, deploymentIdFromDeploymentAnnotation, context.getRequiredTestClass(),
                context.getRequiredTestMethod().getName());
            flowableTestHelper.setDeploymentIdFromDeploymentAnnotation(null);
        }

        processEngine.getProcessEngineConfiguration().getClock().reset();

        FlowableMockSupport mockSupport = flowableTestHelper.getMockSupport();
        if (mockSupport != null) {
            TestHelper.annotationMockSupportTeardown(mockSupport);
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return SUPPORTED_PARAMETERS.contains(parameterType) || FlowableTestHelper.class.equals(parameterType) || FlowableMockSupport.class.equals(parameterType)
            || parameterContext.isAnnotated(DeploymentId.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
        FlowableTestHelper flowableTestHelper = getTestHelper(context);
        if (parameterContext.isAnnotated(DeploymentId.class)) {
            return flowableTestHelper.getDeploymentIdFromDeploymentAnnotation();
        }

        Class<?> parameterType = parameterContext.getParameter().getType();
        ProcessEngine processEngine = flowableTestHelper.getProcessEngine();
        if (parameterType.isInstance(processEngine)) {
            return processEngine;
        } else if (FlowableTestHelper.class.equals(parameterType)) {
            return flowableTestHelper;
        } else if (FlowableMockSupport.class.equals(parameterType)) {
            return flowableTestHelper.getMockSupport();
        }

        try {
            return ProcessEngine.class.getDeclaredMethod("get" + parameterType.getSimpleName()).invoke(processEngine);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
            throw new ParameterResolutionException("Could not find service " + parameterType, ex);
        }
    }

    protected String getConfigurationResource(ExtensionContext context) {
        return AnnotationSupport.findAnnotation(context.getTestClass(), ConfigurationResource.class)
            .map(ConfigurationResource::value)
            .orElse(DEFAULT_CONFIGURATION_RESOURCE);
    }

    protected FlowableTestHelper getTestHelper(ExtensionContext context) {
        return getStore(context)
            .getOrComputeIfAbsent(context.getRequiredTestClass(), key -> new FlowableTestHelper(createProcessEngine(context)), FlowableTestHelper.class);
    }

    protected ProcessEngine createProcessEngine(ExtensionContext context) {
        return TestHelper.getProcessEngine(getConfigurationResource(context));
    }

    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
