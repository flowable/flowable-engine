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
package org.flowable.engine.test.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.bpmn.exceptions.XMLException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class DeployInvalidXmlTest extends PluggableFlowableTestCase {

    @BeforeEach
    protected void setUp() throws Exception {

        processEngineConfiguration.setEnableSafeBpmnXml(true); // Needs to be enabled to test this
    }

    @AfterEach
    protected void tearDown() throws Exception {
        processEngineConfiguration.setEnableSafeBpmnXml(false); // set back to default
    }

    @Test
    public void testDeployNonSchemaConformantXml() {
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/repository/nonSchemaConformantXml.bpmn20.xml").deploy().getId())
                .isInstanceOf(XMLException.class);
    }

    @Test
    public void testDeployWithMissingWaypointsForSequenceflowInDiagramInterchange() {
        assertThatThrownBy(() -> repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/repository/noWayPointsForSequenceFlowInDiagramInterchange.bpmn20.xml").deploy().getId())
                .isInstanceOf(XMLException.class);
    }

    // Need to put this in a String here, if we use a separate file, the cpu usage
    // of Eclipse skyrockets, regardless of the file is opened or not

    private static final String UNSAFE_XML = "<?xml version='1.0' encoding='UTF-8'?>" + "<!-- Billion Laugh attacks : http://portal.sliderocket.com/CJAKM/xml-attacks -->" + "<!DOCTYPE lols ["
            + "<!ENTITY lol 'lol'>" + "<!ENTITY lol1 '&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;'>" + "<!ENTITY lol2 '&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;'>"
            + "<!ENTITY lol3 '&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;'>" + "<!ENTITY lol4 '&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;'>"
            + "<!ENTITY lol5 '&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;'>" + "<!ENTITY lol6 '&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;'>"
            + "<!ENTITY lol7 '&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;'>" + "<!ENTITY lol8 '&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;'>"
            + "<!ENTITY lol9 '&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;'>" + "]>" + "<lolz>&lol9;</lolz>" + "<definitions " + "xmlns='http://www.omg.org/spec/BPMN/20100524/MODEL'"
            + "xmlns:activiti='http://activiti.org/bpmn'" + "targetNamespace='Examples'>" + "<process id='oneTaskProcess' name='The One org.flowable.task.service.Task Process'>"
            + "  <documentation>This is a process for testing purposes</documentation>" + " <startEvent id='theStart' />" + " <sequenceFlow id='flow1' sourceRef='theStart' targetRef='theTask' />"
            + " <userTask id='theTask' name='my task' />" + " <sequenceFlow id='flow2' sourceRef='theTask' targetRef='theEnd' />" + " <endEvent id='theEnd' />" + "</process>" + "</definitions>";

    // See https://activiti.atlassian.net/browse/ACT-1579?focusedCommentId=319886#comment-319886
    @Test
    public void testProcessEngineDenialOfServiceAttackUsingUnsafeXmlTest() throws InterruptedException {

        // Putting this in a Runnable so we can time it out
        // Without safe xml, this would run forever
        MyRunnable runnable = new MyRunnable(repositoryService);
        Thread thread = new Thread(runnable);
        thread.start();

        long waitTime = 60000L;
        thread.join(waitTime);

        assertThat(runnable.finished).isTrue();

    }

    static class MyRunnable implements Runnable {

        public boolean finished;

        protected RepositoryService repositoryService;

        public MyRunnable(RepositoryService repositoryService) {
            this.repositoryService = repositoryService;
        }

        @Override
        public void run() {
            try {
                assertThatThrownBy(() -> {
                    String deploymentId = repositoryService.createDeployment().addString("test.bpmn20.xml", UNSAFE_XML).deploy().getId();
                    assertThat(repositoryService.createProcessDefinitionQuery().singleResult()).isEqualTo(1);
                    repositoryService.deleteDeployment(deploymentId, true);
                })
                        .isInstanceOf(Exception.class);
            }
            finally {
                finished = true;
            }
        }

    }

    @Test
    public void testExternalEntityResolvingTest() {
        String deploymentId = repositoryService.createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/repository/DeployInvalidXmlTest.testExternalEntityResolvingTest.bpmn20.xml")
                .deploy()
                .getId();

        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
            assertThat(processDefinition.getDescription()).isEqualTo("Test 1 2 3 null");
        } finally {
            repositoryService.deleteDeployment(deploymentId, true);
        }
    }

}
