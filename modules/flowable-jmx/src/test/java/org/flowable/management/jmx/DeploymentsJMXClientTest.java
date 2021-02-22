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

package org.flowable.management.jmx;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.net.URL;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.junit.Test;

/**
 * @author Saeid Mirzaei
 */

public class DeploymentsJMXClientTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testDeploymentsJmxClient() throws Exception {
        String hostName = Utils.getHostName();
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://" + hostName + ":10111/jndi/rmi://" + hostName + ":1099/jmxrmi/flowable");

        ProcessEngineConfiguration processEngineConfig = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("flowable.cfg.xml");
        ProcessEngine processEngine = processEngineConfig.buildProcessEngine();

        // wait for jmx server to come up
        Thread.sleep(500);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);

        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

        ObjectName deploymentsBeanName = new ObjectName("org.flowable.jmx.Mbeans:type=Deployments");

        Thread.sleep(500);

        // no process deployed yet
        List<List<String>> deployments = (List<List<String>>) mbsc.getAttribute(deploymentsBeanName, "Deployments");
        assertThat(deployments).isEmpty();

        // deploy process remotely

        URL fileName = Thread.currentThread().getContextClassLoader().getResource("org/flowable/management/jmx/trivialProcess.bpmn");
        mbsc.invoke(deploymentsBeanName, "deployProcessDefinition", new String[] { "trivialProcess.bpmn", fileName.getFile() }, new String[] { String.class.getName(), String.class.getName() });

        // one process is there now, test remote deployments
        deployments = (List<List<String>>) mbsc.getAttribute(deploymentsBeanName, "Deployments");
        assertThat(deployments).hasSize(1);
        assertThat(deployments.get(0)).hasSize(3);
        String firstDeploymentId = deployments.get(0).get(0);

        // test remote process definition
        List<List<String>> pdList = (List<List<String>>) mbsc.getAttribute(deploymentsBeanName, "ProcessDefinitions");
        assertThat(pdList).hasSize(1);
        assertThat(pdList.get(0)).hasSize(5);
        assertThat(pdList.get(0).get(0)).isNotNull();
        assertThat(pdList.get(0).get(1)).isEqualTo("My process");
        assertThat(pdList.get(0).get(2)).isEqualTo("1"); // version
        assertThat(pdList.get(0).get(3)).isEqualTo("false"); // not suspended
        assertThat(pdList.get(0).get(4)).isEqualTo("This process to test JMX");

        // redeploy the same process
        mbsc.invoke(deploymentsBeanName, "deployProcessDefinition", new String[] { "trivialProcess.bpmn", fileName.getFile() }, new String[] { String.class.getName(), String.class.getName() });

        // now there should be two deployments
        deployments = (List<List<String>>) mbsc.getAttribute(deploymentsBeanName, "Deployments");
        assertThat(deployments).hasSize(2);
        assertThat(deployments.get(0)).hasSize(3);
        assertThat(deployments.get(1)).hasSize(3);

        // there should be two process definitions, one with version equals to
        // two
        pdList = (List<List<String>>) mbsc.getAttribute(deploymentsBeanName, "ProcessDefinitions");
        assertThat(pdList).hasSize(2);
        assertThat(pdList.get(0)).hasSize(5);
        assertThat(pdList.get(1)).hasSize(5);

        // check there is one with version= = 1 and another one with version ==
        // 2, other attributed are the same
        String pidV2 = null;
        String pidV1 = null;
        if ("1".equals(pdList.get(0).get(2)) && "2".equals(pdList.get(1).get(2))) {
            pidV2 = pdList.get(1).get(0);
            pidV1 = pdList.get(0).get(0);
        } else if ("1".equals(pdList.get(1).get(2)) && "2".equals(pdList.get(0).get(2))) {
            pidV2 = pdList.get(0).get(0);
            pidV1 = pdList.get(1).get(0);

        } else
            fail("there should one process definition with version == 1 and another one with version == 2. It is not the case");

        assertThat(pdList.get(0).get(0)).isNotNull();
        assertThat(pdList.get(1).get(0)).isNotNull();
        assertThat(pdList.get(0).get(1)).isEqualTo("My process");
        assertThat(pdList.get(1).get(1)).isEqualTo("My process");
        assertThat(pdList.get(0).get(3)).isEqualTo("false"); // not suspended
        assertThat(pdList.get(1).get(3)).isEqualTo("false"); // not suspended
        assertThat(pdList.get(0).get(4)).isEqualTo("This process to test JMX");
        assertThat(pdList.get(1).get(4)).isEqualTo("This process to test JMX");

        // suspend the one with version == 2
        mbsc.invoke(deploymentsBeanName, "suspendProcessDefinitionById", new String[] { pidV2 }, new String[] { String.class.getName() });
        RepositoryService repositoryService = processEngine.getRepositoryService();

        // test if it is really suspended and not the other one
        assertThat(repositoryService.getProcessDefinition(pidV2).isSuspended()).isTrue();
        assertThat(repositoryService.getProcessDefinition(pidV1).isSuspended()).isFalse();

        // test if it is reported as suspended and not the other one
        List<String> pd = (List<String>) mbsc.invoke(deploymentsBeanName, "getProcessDefinitionById", new String[] { pidV2 }, new String[] { String.class.getName() });
        assertThat(pd).hasSize(5);
        assertThat(pd.get(3)).isEqualTo("true");

        pd = (List<String>) mbsc.invoke(deploymentsBeanName, "getProcessDefinitionById", new String[] { pidV1 }, new String[] { String.class.getName() });
        assertThat(pd).hasSize(5);
        assertThat(pd.get(3)).isEqualTo("false");

        // now reactivate the same suspended process
        mbsc.invoke(deploymentsBeanName, "activatedProcessDefinitionById", new String[] { pidV2 }, new String[] { String.class.getName() });

        // test if both processes are active again
        assertThat(repositoryService.getProcessDefinition(pidV2).isSuspended()).isFalse();
        assertThat(repositoryService.getProcessDefinition(pidV1).isSuspended()).isFalse();

        // test if they are properly reported as activated

        pd = (List<String>) mbsc.invoke(deploymentsBeanName, "getProcessDefinitionById", new String[] { pidV2 }, new String[] { String.class.getName() });
        assertThat(pd).hasSize(5);
        assertThat(pd.get(3)).isEqualTo("false");

        pd = (List<String>) mbsc.invoke(deploymentsBeanName, "getProcessDefinitionById", new String[] { pidV1 }, new String[] { String.class.getName() });
        assertThat(pd).hasSize(5);
        assertThat(pd.get(3)).isEqualTo("false");

        // now undeploy the one with version == 1
        mbsc.invoke(deploymentsBeanName, "deleteDeployment", new String[] { firstDeploymentId }, new String[] { String.class.getName() });

        // now there should be only one deployment and only one process
        // definition with version 2, first check it with API
        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);

        assertThat(firstDeploymentId).isNotEqualTo(repositoryService.createDeploymentQuery().singleResult().getId());

        // check if it is also affected in returned results.

        deployments = (List<List<String>>) mbsc.getAttribute(deploymentsBeanName, "Deployments");
        assertThat(deployments).hasSize(1);
        assertThat(deployments.get(0)).hasSize(3);
        assertThat(firstDeploymentId).isNotEqualTo(deployments.get(0).get(0));

    }

}
