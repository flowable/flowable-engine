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
package org.flowable.test.scripting.secure;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.repository.Deployment;
import org.flowable.scripting.secure.SecureJavascriptConfigurator;
import org.flowable.scripting.secure.impl.SecureScriptClassShutter;
import org.junit.After;
import org.junit.Before;

/**
 * @author Joram Barrez
 */
public abstract class SecureScriptingBaseTest {

    protected ProcessEngine processEngine;
    protected RuntimeService runtimeService;
    protected RepositoryService repositoryService;
    protected TaskService taskService;
    protected ExposedBean exposedBean = new ExposedBean();

    public static class ExposedBean{
        public int getNumber(){
            return 123;
        }
    }

    @Before
    public void initProcessEngine() {

        SecureJavascriptConfigurator configurator = new SecureJavascriptConfigurator()
                .setWhiteListedClasses(new HashSet<>(Collections.singletonList("java.util.ArrayList")))
                .setMaxStackDepth(10).setMaxScriptExecutionTime(3000L)
                .setMaxMemoryUsed(3145728L)
                .setEnableAccessToBeans(true);

        Map<Object, Object> beans = new HashMap<>();
        beans.put("exposedBean", exposedBean);
        final ProcessEngineConfigurationImpl processEngineConfiguration =
                new StandaloneInMemProcessEngineConfiguration()
                        .addConfigurator(configurator)
                        .setDatabaseSchemaUpdate("create-drop");
        processEngineConfiguration.setBeans(beans);
        this.processEngine = processEngineConfiguration
                .buildProcessEngine();

        this.runtimeService = processEngine.getRuntimeService();
        this.repositoryService = processEngine.getRepositoryService();
        this.taskService = processEngine.getTaskService();
    }

    @After
    public void shutdownProcessEngine() {

        for (Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }

        this.taskService = null;
        this.repositoryService = null;
        this.runtimeService = null;

        this.processEngine.close();
        this.processEngine = null;
    }

    protected void deployProcessDefinition(String classpathResource) {
        repositoryService.createDeployment().addClasspathResource(classpathResource).deploy();
    }

    protected void enableSysoutsInScript() {
        addWhiteListedClass("java.lang.System");
        addWhiteListedClass("java.io.PrintStream");
    }

    protected void addWhiteListedClass(String whiteListedClass) {
        SecureScriptClassShutter secureScriptClassShutter = SecureJavascriptConfigurator.getSecureScriptClassShutter();
        secureScriptClassShutter.addWhiteListedClass(whiteListedClass);
    }

    protected void removeWhiteListedClass(String whiteListedClass) {
        SecureScriptClassShutter secureScriptClassShutter = SecureJavascriptConfigurator.getSecureScriptClassShutter();
        secureScriptClassShutter.removeWhiteListedClass(whiteListedClass);
    }

}
