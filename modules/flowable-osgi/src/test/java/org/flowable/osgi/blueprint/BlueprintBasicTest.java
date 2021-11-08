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
package org.flowable.osgi.blueprint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.inject.Inject;

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.osgi.blueprint.bean.ActivityBehaviourBean;
import org.flowable.osgi.blueprint.bean.SimpleBean;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Test class to do basic testing against an OSGi container using the Flowable blueprint functionality
 * 
 * @author Tijs Rademakers
 */

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class BlueprintBasicTest {

    @Inject
    protected BundleContext ctx;

    @Inject
    protected ProcessEngine processEngine;

    @Inject
    protected RuntimeService runtimeService;

    @Inject
    protected RepositoryService repositoryService;

    @Inject
    protected HistoryService historyService;

    @Configuration
    public Option[] createConfiguration() {
        Option[] coreBundles = options(
                mavenBundle().groupId("org.flowable").artifactId("flowable-bpmn-model").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-engine-common-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-engine-common").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-http-common").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-bpmn-converter").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-process-validation").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-image-generator").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-form-model").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-form-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-dmn-model").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-dmn-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-idm-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-idm-engine").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-idm-engine-configurator").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-event-registry-model").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-event-registry-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-event-registry-json-converter").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-event-registry").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-event-registry-configurator").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-content-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-variable-service-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-variable-service").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-identitylink-service-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-identitylink-service").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-entitylink-service-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-entitylink-service").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-eventsubscription-service-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-eventsubscription-service").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-task-service-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-task-service").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-job-service-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-job-service").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-batch-service").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-batch-service-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-cmmn-model").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-cmmn-api").versionAsInProject(),
                mavenBundle().groupId("org.flowable").artifactId("flowable-engine").versionAsInProject(),
                mavenBundle().groupId("org.apache.commons").artifactId("commons-lang3").versionAsInProject(),
                mavenBundle().groupId("commons-io").artifactId("commons-io").versionAsInProject(),
                mavenBundle().groupId("com.fasterxml.uuid").artifactId("java-uuid-generator").versionAsInProject(),
                mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-core").versionAsInProject(),
                mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-databind").versionAsInProject(),
                mavenBundle().groupId("com.fasterxml.jackson.core").artifactId("jackson-annotations").versionAsInProject(),
                mavenBundle().groupId("joda-time").artifactId("joda-time").versionAsInProject(),
                mavenBundle().groupId("com.h2database").artifactId("h2").versionAsInProject(),
                mavenBundle().groupId("org.mybatis").artifactId("mybatis").versionAsInProject(),
                mavenBundle().groupId("org.liquibase").artifactId("liquibase-core").versionAsInProject(),
                mavenBundle().groupId("org.slf4j").artifactId("slf4j-log4j12").versionAsInProject().noStart(),
                mavenBundle().groupId("org.junit.jupiter").artifactId("junit-jupiter-api").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.fileinstall").versionAsInProject(),
                mavenBundle().groupId("org.apache.aries.blueprint").artifactId("org.apache.aries.blueprint.core").versionAsInProject(),
                mavenBundle().groupId("org.apache.aries.proxy").artifactId("org.apache.aries.proxy").versionAsInProject(),
                mavenBundle().groupId("org.apache.aries").artifactId("org.apache.aries.util").versionAsInProject(),
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.enterprise").versionAsInProject(),
                bundle("reference:file:target/classes"));

        Option[] optionArray = OptionUtils.combine(coreBundles, CoreOptions.junitBundles(),
                provision(createTestBundleWithProcessEngineConfiguration(), createTestBundleWithProcessDefinition(), createTestBundleWithTask()));
        return optionArray;
    }

    protected InputStream createTestBundleWithProcessEngineConfiguration() {
        try {
            return TinyBundles.bundle().add("OSGI-INF/blueprint/context.xml", new FileInputStream(new File("src/test/resources/config/context.xml")))
                    .set(Constants.BUNDLE_SYMBOLICNAME, "org.flowable.osgi.config").set(Constants.DYNAMICIMPORT_PACKAGE, "*").build();
        } catch (FileNotFoundException fnfe) {
            fail("Failure in createTestBundleWithProcessEngineConfiguration " + fnfe);
            return null;
        }
    }

    protected InputStream createTestBundleWithProcessDefinition() {
        try {
            return TinyBundles.bundle().add("OSGI-INF/flowable/example.bpmn20.xml", new FileInputStream(new File("src/test/resources/processes/example.bpmn20.xml")))
                    .set(Constants.BUNDLE_SYMBOLICNAME, "org.flowable.osgi.example").build();
        } catch (FileNotFoundException fnfe) {
            fail("Failure in createTestBundleWithProcessDefinition " + fnfe);
            return null;
        }
    }

    protected InputStream createTestBundleWithTask() {
        try {
            return TinyBundles.bundle().add("OSGI-INF/blueprint/context.xml", new FileInputStream(new File("src/test/resources/task/context.xml"))).add(SimpleBean.class).add(ActivityBehaviourBean.class)
                    .set(Constants.BUNDLE_SYMBOLICNAME, "org.flowable.osgi.task").set(Constants.DYNAMICIMPORT_PACKAGE, "*").build();
        } catch (FileNotFoundException fnfe) {
            fail("Failure in createTestBundleWithTask " + fnfe);
            return null;
        }
    }

    @Test
    public void exportedServices() throws Exception {
        assertNotNull(processEngine);
        assertNotNull(repositoryService);
        // wait for deployment to be done
        Thread.sleep(5000);
        Deployment deployment = repositoryService.createDeploymentQuery().singleResult();
        assertEquals("org.flowable.osgi.example", deployment.getName());
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertEquals("osgiProcess", processDefinition.getKey());
    }

    @Test
    public void exportJavaDelegate() throws Exception {
        // wait for deployment to be done
        Thread.sleep(5000);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("osgiProcess");
        assertTrue(processInstance.isEnded());
        HistoricVariableInstance variable = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("visited").singleResult();
        assertTrue((Boolean) variable.getValue());
        HistoricVariableInstance activityBehaviourVisited = historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("visitedActivityBehaviour")
                .singleResult();
        assertTrue((Boolean) activityBehaviourVisited.getValue());
    }
}
