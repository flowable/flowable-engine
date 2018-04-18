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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.db.DbSchemaManager;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.dmn.api.DmnDeploymentBuilder;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.deployer.DmnResourceUtil;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public abstract class DmnTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmnTestHelper.class);

    public static final String EMPTY_LINE = "\n";

    static Map<String, DmnEngine> dmnEngines = new HashMap<>();
    
    private static final List<String> TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK = new ArrayList<>();
    
    static {
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_DMN_DATABASECHANGELOG");
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_DMN_DATABASECHANGELOGLOCK");
    }

    // Test annotation support /////////////////////////////////////////////

    public static String annotationDeploymentSetUp(DmnEngine dmnEngine, Class<?> testClass, String methodName) {
        String deploymentId = null;
        Method method = null;
        try {
            method = testClass.getMethod(methodName, (Class<?>[]) null);
        } catch (Exception e) {
            LOGGER.warn("Could not get method by reflection. This could happen if you are using @Parameters in combination with annotations.", e);
            return null;
        }
        
        DmnDeployment deploymentAnnotation = method.getAnnotation(DmnDeployment.class);
        if (deploymentAnnotation != null) {
            deploymentId = deployResourceFromAnnotation(dmnEngine, testClass, methodName, method, deploymentAnnotation.resources());
        }
        DmnDeploymentAnnotation deploymentAnnotation2 = method.getAnnotation(DmnDeploymentAnnotation.class);
        if (deploymentAnnotation2 != null) {
            deploymentId = deployResourceFromAnnotation(dmnEngine, testClass, methodName, method, deploymentAnnotation2.resources());
        }

        return deploymentId;
    }

    protected static String deployResourceFromAnnotation(DmnEngine dmnEngine, Class<?> testClass, String methodName,
            Method method, String[] resources) {
        String deploymentId;
        LOGGER.debug("annotation @Deployment creates deployment for {}.{}", testClass.getSimpleName(), methodName);
        if (resources.length == 0) {
            String name = method.getName();
            String resource = getDmnDecisionResource(testClass, name);
            resources = new String[] { resource };
        }

        DmnDeploymentBuilder deploymentBuilder = dmnEngine.getDmnRepositoryService().createDeployment().name(testClass.getSimpleName() + "." + methodName);

        for (String resource : resources) {
            deploymentBuilder.addClasspathResource(resource);
        }

        deploymentId = deploymentBuilder.deploy().getId();
        return deploymentId;
    }

    public static void annotationDeploymentTearDown(DmnEngine dmnEngine, String deploymentId, Class<?> testClass, String methodName) {
        LOGGER.debug("annotation @Deployment deletes deployment for {}.{}", testClass.getSimpleName(), methodName);
        if (deploymentId != null) {
            try {
                dmnEngine.getDmnRepositoryService().deleteDeployment(deploymentId);
            } catch (FlowableObjectNotFoundException e) {
                // Deployment was already deleted by the test case. Ignore.
            }
        }
    }

    /**
     * get a resource location by convention based on a class (type) and a relative resource name. The return value will be the full classpath location of the type, plus a suffix built from the name
     * parameter: <code>DmnDeployer.DMN_RESOURCE_SUFFIXES</code>. The first resource matching a suffix will be returned.
     */
    public static String getDmnDecisionResource(Class<?> type, String name) {
        for (String suffix : DmnResourceUtil.DMN_RESOURCE_SUFFIXES) {
            String resource = type.getName().replace('.', '/') + "." + name + "." + suffix;
            InputStream inputStream = DmnTestHelper.class.getClassLoader().getResourceAsStream(resource);
            if (inputStream == null) {
                continue;
            } else {
                return resource;
            }
        }
        return type.getName().replace('.', '/') + "." + name + "." + DmnResourceUtil.DMN_RESOURCE_SUFFIXES[0];
    }

    // Engine startup and shutdown helpers
    // ///////////////////////////////////////////////////

    public static DmnEngine getDmnEngine(String configurationResource) {
        DmnEngine dmnEngine = dmnEngines.get(configurationResource);
        if (dmnEngine == null) {
            LOGGER.debug("==== BUILDING DMN ENGINE ========================================================================");
            dmnEngine = DmnEngineConfiguration.createDmnEngineConfigurationFromResource(configurationResource).setDatabaseSchemaUpdate(DmnEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE).buildDmnEngine();
            LOGGER.debug("==== DMN ENGINE CREATED =========================================================================");
            dmnEngines.put(configurationResource, dmnEngine);
        }
        return dmnEngine;
    }

    public static void closeDmnEngines() {
        for (DmnEngine dmnEngine : dmnEngines.values()) {
            dmnEngine.close();
        }
        dmnEngines.clear();
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    public static void assertAndEnsureCleanDb(final DmnEngine dmnEngine) {
        LOGGER.debug("verifying that db is clean after test");
        DmnEngineConfiguration dmnEngineConfiguration = dmnEngine.getDmnEngineConfiguration();
        DmnManagementService managementService = dmnEngine.getDmnManagementService();
        Map<String, Long> tableCounts = managementService.getTableCount();
        StringBuilder outputMessage = new StringBuilder();
        for (String tableName : tableCounts.keySet()) {
            String tableNameWithoutPrefix = tableName.replace(dmnEngineConfiguration.getDatabaseTablePrefix(), "");
            if (!TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.contains(tableNameWithoutPrefix)) {
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

            LOGGER.info("dropping and recreating db");

            CommandExecutor commandExecutor = dmnEngine.getDmnEngineConfiguration().getCommandExecutor();
            CommandConfig config = new CommandConfig().transactionNotSupported();
            commandExecutor.execute(config, new Command<Object>() {
                @Override
                public Object execute(CommandContext commandContext) {
                    DbSchemaManager dbSchemaManager = CommandContextUtil.getDmnEngineConfiguration().getDbSchemaManager();
                    dbSchemaManager.dbSchemaDrop();
                    dbSchemaManager.dbSchemaCreate();
                    return null;
                }
            });

            Assert.fail(outputMessage.toString());
            
        } else {
            LOGGER.info("database was clean");
        }
    }

}
