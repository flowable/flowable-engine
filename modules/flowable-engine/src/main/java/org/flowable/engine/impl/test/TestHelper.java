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

package org.flowable.engine.impl.test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.bpmn.deployer.ResourceNameUtil;
import org.flowable.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.TestActivityBehaviorFactory;
import org.flowable.engine.test.mock.FlowableMockSupport;
import org.flowable.engine.test.mock.MockServiceTask;
import org.flowable.engine.test.mock.MockServiceTasks;
import org.flowable.engine.test.mock.NoOpServiceTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class TestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestHelper.class);

    public static final String EMPTY_LINE = "\n";

    public static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = Collections.singletonList("ACT_GE_PROPERTY");

    static Map<String, ProcessEngine> processEngines = new HashMap<>();

    // Assertion methods ///////////////////////////////////////////////////

    public static void assertProcessEnded(ProcessEngine processEngine, String processInstanceId) {
        ProcessInstance processInstance = processEngine.getRuntimeService().createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

        if (processInstance != null) {
            throw new AssertionError("expected finished process instance '" + processInstanceId + "' but it was still in the db");
        }
    }

    // Test annotation support /////////////////////////////////////////////

    public static String annotationDeploymentSetUp(ProcessEngine processEngine, Class<?> testClass, String methodName) {
        Method method = null;
        try {
            method = testClass.getMethod(methodName, (Class<?>[]) null);
        } catch (Exception e) {
            LOGGER.warn("Could not get method by reflection. This could happen if you are using @Parameters in combination with annotations.", e);
            return null;
        }
        return annotationDeploymentSetUp(processEngine, testClass, method);
    }

    public static String annotationDeploymentSetUp(ProcessEngine processEngine, Class<?> testClass, Method method) {
        Deployment deploymentAnnotation = method.getAnnotation(Deployment.class);
        return annotationDeploymentSetUp(processEngine, testClass, method, deploymentAnnotation);
    }

    public static String annotationDeploymentSetUp(ProcessEngine processEngine, Class<?> testClass, Method method, Deployment deploymentAnnotation) {
        String deploymentId = null;
        String methodName = method.getName();
        if (deploymentAnnotation != null) {
            LOGGER.debug("annotation @Deployment creates deployment for {}.{}", testClass.getSimpleName(), methodName);
            String[] resources = deploymentAnnotation.resources();
            if (resources.length == 0) {
                String name = method.getName();
                String resource = getBpmnProcessDefinitionResource(testClass, name);
                resources = new String[] { resource };
            }

            DeploymentBuilder deploymentBuilder = processEngine.getRepositoryService().createDeployment().name(testClass.getSimpleName() + "." + methodName);

            for (String resource : resources) {
                deploymentBuilder.addClasspathResource(resource);
            }

            if (deploymentAnnotation.tenantId() != null
                    && deploymentAnnotation.tenantId().length() > 0) {
                deploymentBuilder.tenantId(deploymentAnnotation.tenantId());
            }

            deploymentId = deploymentBuilder.deploy().getId();
        }

        return deploymentId;
    }

    public static void annotationDeploymentTearDown(ProcessEngine processEngine, String deploymentId, Class<?> testClass, String methodName) {
        LOGGER.debug("annotation @Deployment deletes deployment for {}.{}", testClass.getSimpleName(), methodName);
        if (deploymentId != null) {
            try {
                processEngine.getRepositoryService().deleteDeployment(deploymentId, true);
            
            } catch (FlowableObjectNotFoundException e) {
                // Deployment was already deleted by the test case. Ignore.
            }
        }
    }

    public static void annotationMockSupportSetup(Class<?> testClass, String methodName, FlowableMockSupport mockSupport) {

        // Get method
        Method method = null;
        try {
            method = testClass.getMethod(methodName, (Class<?>[]) null);
        } catch (Exception e) {
            LOGGER.warn("Could not get method by reflection. This could happen if you are using @Parameters in combination with annotations.", e);
            return;
        }

        handleMockServiceTaskAnnotation(mockSupport, method);
        handleMockServiceTasksAnnotation(mockSupport, method);
        handleNoOpServiceTasksAnnotation(mockSupport, method);
    }

    protected static void handleMockServiceTaskAnnotation(FlowableMockSupport mockSupport, Method method) {
        MockServiceTask mockedServiceTask = method.getAnnotation(MockServiceTask.class);
        if (mockedServiceTask != null) {
            handleMockServiceTaskAnnotation(mockSupport, mockedServiceTask);
        }
    }

    public static void handleMockServiceTaskAnnotation(FlowableMockSupport mockSupport, MockServiceTask mockedServiceTask) {
        String originalClassName = mockedServiceTask.originalClassName();
        mockSupport.mockServiceTaskWithClassDelegate(originalClassName, mockedServiceTask.mockedClassName());
        Class<?> mockedClass = mockedServiceTask.mockedClass();
        if (!Void.class.equals(mockedClass)) {
            mockSupport.mockServiceTaskWithClassDelegate(originalClassName, mockedClass);
        }

        String id = mockedServiceTask.id();
        if (!id.isEmpty()) {
            mockSupport.mockServiceTaskByIdWithClassDelegate(id, mockedServiceTask.mockedClassName());
            if (!Void.class.equals(mockedClass)) {
                mockSupport.mockServiceTaskByIdWithClassDelegate(id, mockedClass);
            }
        }
    }

    protected static void handleMockServiceTasksAnnotation(FlowableMockSupport mockSupport, Method method) {
        MockServiceTasks mockedServiceTasks = method.getAnnotation(MockServiceTasks.class);
        if (mockedServiceTasks != null) {
            for (MockServiceTask mockedServiceTask : mockedServiceTasks.value()) {
                handleMockServiceTaskAnnotation(mockSupport, mockedServiceTask);
            }
        }
    }

    protected static void handleNoOpServiceTasksAnnotation(FlowableMockSupport mockSupport, Method method) {
        NoOpServiceTasks noOpServiceTasks = method.getAnnotation(NoOpServiceTasks.class);
        if (noOpServiceTasks != null) {
            handleNoOpServiceTasksAnnotation(mockSupport, noOpServiceTasks);
        }
    }

    public static void handleNoOpServiceTasksAnnotation(FlowableMockSupport mockSupport, NoOpServiceTasks noOpServiceTasks) {
        if (noOpServiceTasks != null) {
            String[] ids = noOpServiceTasks.ids();
            Class<?>[] classes = noOpServiceTasks.classes();
            String[] classNames = noOpServiceTasks.classNames();

            if ((ids == null || ids.length == 0) && (classes == null || classes.length == 0) && (classNames == null || classNames.length == 0)) {
                mockSupport.setAllServiceTasksNoOp();
            } else {

                if (ids != null && ids.length > 0) {
                    for (String id : ids) {
                        mockSupport.addNoOpServiceTaskById(id);
                    }
                }

                if (classes != null && classes.length > 0) {
                    for (Class<?> clazz : classes) {
                        mockSupport.addNoOpServiceTaskByClassName(clazz.getName());
                    }
                }

                if (classNames != null && classNames.length > 0) {
                    for (String className : classNames) {
                        mockSupport.addNoOpServiceTaskByClassName(className);
                    }
                }

            }
        }
    }

    public static void annotationMockSupportTeardown(FlowableMockSupport mockSupport) {
        mockSupport.reset();
    }

    /**
     * get a resource location by convention based on a class (type) and a relative resource name. The return value will be the full classpath location of the type, plus a suffix built from the name
     * parameter: <code>BpmnDeployer.BPMN_RESOURCE_SUFFIXES</code>. The first resource matching a suffix will be returned.
     */
    public static String getBpmnProcessDefinitionResource(Class<?> type, String name) {
        for (String suffix : ResourceNameUtil.BPMN_RESOURCE_SUFFIXES) {
            String resource = type.getName().replace('.', '/') + "." + name + "." + suffix;
            InputStream inputStream = ReflectUtil.getResourceAsStream(resource);
            if (inputStream == null) {
                continue;
            } else {
                return resource;
            }
        }
        return type.getName().replace('.', '/') + "." + name + "." + ResourceNameUtil.BPMN_RESOURCE_SUFFIXES[0];
    }

    // Engine startup and shutdown helpers
    // ///////////////////////////////////////////////////

    public static ProcessEngine getProcessEngine(String configurationResource) {
        ProcessEngine processEngine = processEngines.get(configurationResource);
        if (processEngine == null) {
            LOGGER.debug("==== BUILDING PROCESS ENGINE ========================================================================");
            processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource(configurationResource).buildProcessEngine();
            LOGGER.debug("==== PROCESS ENGINE CREATED =========================================================================");
            processEngines.put(configurationResource, processEngine);
        }
        return processEngine;
    }

    public static void closeProcessEngines() {
        for (ProcessEngine processEngine : processEngines.values()) {
            processEngine.close();
        }
        processEngines.clear();
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    public static void assertAndEnsureCleanDb(ProcessEngine processEngine) {
        LOGGER.debug("verifying that db is clean after test");
        Map<String, Long> tableCounts = processEngine.getManagementService().getTableCount();
        StringBuilder outputMessage = new StringBuilder();
        for (String tableName : tableCounts.keySet()) {
            if (!TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.contains(tableName)) {
                Long count = tableCounts.get(tableName);
                if (count != 0L) {
                    outputMessage.append("  ").append(tableName).append(": ").append(count).append(" record(s) ");
                }
            }
        }
        if (outputMessage.length() > 0) {
            outputMessage.insert(0, "DB NOT CLEAN: \n");
            LOGGER.error(EMPTY_LINE);
            LOGGER.error(outputMessage.toString());

            ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration().getCommandExecutor().execute(new Command<Object>() {
                @Override
                public Object execute(CommandContext commandContext) {
                    SchemaManager dbSchemaManager = CommandContextUtil.getProcessEngineConfiguration(commandContext).getDbSchemaManager();
                    dbSchemaManager.schemaDrop();
                    dbSchemaManager.schemaCreate();
                    return null;
                }
            });

            throw new AssertionError(outputMessage.toString());
        }
    }

    // Mockup support ////////////////////////////////////////////////////////

    public static TestActivityBehaviorFactory initializeTestActivityBehaviorFactory(ActivityBehaviorFactory existingActivityBehaviorFactory) {
        return new TestActivityBehaviorFactory(existingActivityBehaviorFactory);
    }

}
