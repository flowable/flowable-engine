/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this    except in compliance with the License.
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

package org.flowable.management.jmx.mbeans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.management.jmx.DefaultManagementMBeanAssembler;
import org.flowable.management.jmx.ManagementMBeanAssembler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Saeid Mirzaei
 */

public class ProcessDefinitionsTest {

    protected ProcessDefinitionsMBean processDefinitionsMBean;

    @Mock
    protected ProcessEngineConfiguration processEngineConfiguration;

    @Mock
    protected RepositoryService repositoryService;

    @Mock
    protected ProcessDefinitionQuery processDefinitionQuery;

    @Mock
    protected DeploymentQuery deploymentQuery;

    @Mock
    protected DeploymentBuilder deploymentBuilder;

    protected ManagementMBeanAssembler assembler = new DefaultManagementMBeanAssembler();

    @Before
    public void initMocks() throws MalformedObjectNameException {
        MockitoAnnotations.initMocks(this);
        when(processEngineConfiguration.getRepositoryService()).thenReturn(repositoryService);
        processDefinitionsMBean = new ProcessDefinitionsMBean(processEngineConfiguration);
    }

    @Test
    public void testGetProcessDefinitions() {

        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        List<ProcessDefinition> processDefinitionList = new ArrayList<>();
        ProcessDefinitionEntity pd = new ProcessDefinitionEntityImpl();
        pd.setId("testId");
        pd.setName("testName");
        pd.setVersion(175);
        pd.setSuspensionState(1);
        pd.setDescription("testDescription");

        processDefinitionList.add(pd);

        when(processDefinitionQuery.list()).thenReturn(processDefinitionList);

        List<List<String>> result = processDefinitionsMBean.getProcessDefinitions();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(5);
        assertThat(result.get(0).get(0)).isEqualTo("testId");
        assertThat(result.get(0).get(1)).isEqualTo("testName");
        assertThat(result.get(0).get(2)).isEqualTo("175");
        assertThat(result.get(0).get(3)).isEqualTo("false");
        assertThat(result.get(0).get(4)).isEqualTo("testDescription");

        pd.setSuspensionState(2);

        result = processDefinitionsMBean.getProcessDefinitions();
        assertThat(result.get(0).get(3)).isEqualTo("true");

    }

    @Test
    public void testDeployments() {
        when(repositoryService.createDeploymentQuery()).thenReturn(deploymentQuery);
        DeploymentEntity deployment = new DeploymentEntityImpl();
        List<Deployment> deploymentList = new ArrayList<>();
        deployment.setId("testDeploymentId");
        deployment.setName("testDeploymentName");
        deployment.setTenantId("tenantId");
        deploymentList.add(deployment);
        when(deploymentQuery.list()).thenReturn(deploymentList);

        List<List<String>> result = processDefinitionsMBean.getDeployments();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).hasSize(3);
        assertThat(result.get(0).get(0)).isEqualTo("testDeploymentId");
        assertThat(result.get(0).get(1)).isEqualTo("testDeploymentName");
        assertThat(result.get(0).get(2)).isEqualTo("tenantId");

    }

    @Test
    public void testDeleteDeployment() {
        processDefinitionsMBean.deleteDeployment("id");
        verify(repositoryService).deleteDeployment("id");
    }

    @Test
    public void testSuspendProcessDefinitionById() {
        processDefinitionsMBean.suspendProcessDefinitionById("id");
        verify(repositoryService).suspendProcessDefinitionById("id");
    }

    @Test
    public void testActivatedProcessDefinitionById() {
        processDefinitionsMBean.activatedProcessDefinitionById("id");
        verify(repositoryService).activateProcessDefinitionById("id");
    }

    @Test
    public void testSuspendProcessDefinitionByKey() {
        processDefinitionsMBean.suspendProcessDefinitionByKey("id");
        verify(repositoryService).suspendProcessDefinitionByKey("id");
    }

    @Test
    public void testActivatedProcessDefinitionByKey() {
        processDefinitionsMBean.activatedProcessDefinitionByKey("id");
        verify(repositoryService).activateProcessDefinitionByKey("id");
    }

    @Test
    public void testAnnotations() throws MalformedObjectNameException, JMException {

        ModelMBean modelBean = assembler.assemble(processDefinitionsMBean, new ObjectName("domain", "key", "value"));
        assertThat(modelBean).isNotNull();
        MBeanInfo beanInfo = modelBean.getMBeanInfo();
        assertThat(beanInfo).isNotNull();
        assertThat(beanInfo.getOperations()).hasSize(9);
        int counter = 0;

        for (MBeanOperationInfo op : beanInfo.getOperations()) {
            if ("deleteDeployment".equals(op.getName())) {
                counter++;
                assertThat(op.getDescription()).isEqualTo("delete deployment");
                assertThat(op.getReturnType()).isEqualTo("void");
                assertThat(op.getSignature()).hasSize(1);
                assertThat(op.getSignature()[0].getType()).isEqualTo("java.lang.String");
            } else if ("suspendProcessDefinitionById".equals(op.getName())) {
                counter++;
                assertThat(op.getDescription()).isEqualTo("Suspend given process ID");
                assertThat(op.getReturnType()).isEqualTo("void");
                assertThat(op.getSignature()).hasSize(1);
                assertThat(op.getSignature()[0].getType()).isEqualTo("java.lang.String");
            } else if ("activatedProcessDefinitionById".equals(op.getName())) {
                counter++;
                assertThat(op.getDescription()).isEqualTo("Activate given process ID");
                assertThat(op.getReturnType()).isEqualTo("void");
                assertThat(op.getSignature()).hasSize(1);
                assertThat(op.getSignature()[0].getType()).isEqualTo("java.lang.String");
            } else if ("suspendProcessDefinitionByKey".equals(op.getName())) {
                counter++;
                assertThat(op.getDescription()).isEqualTo("Suspend given process ID");
                assertThat(op.getReturnType()).isEqualTo("void");
                assertThat(op.getSignature()).hasSize(1);
                assertThat(op.getSignature()[0].getType()).isEqualTo("java.lang.String");
            } else if ("activatedProcessDefinitionByKey".equals(op.getName())) {
                counter++;
                assertThat(op.getDescription()).isEqualTo("Activate given process ID");
                assertThat(op.getReturnType()).isEqualTo("void");
                assertThat(op.getSignature()).hasSize(1);
                assertThat(op.getSignature()[0].getType()).isEqualTo("java.lang.String");
            } else if ("deployProcessDefinition".equals(op.getName())) {
                counter++;
                assertThat(op.getDescription()).isEqualTo("Deploy Process Definition");
                assertThat(op.getReturnType()).isEqualTo("void");
                assertThat(op.getSignature()).hasSize(2);
                assertThat(op.getSignature()[0].getType()).isEqualTo("java.lang.String");
                assertThat(op.getSignature()[1].getType()).isEqualTo("java.lang.String");
            }

        }
        assertThat(counter).isEqualTo(6);

        // check attributes
        assertThat(beanInfo.getAttributes()).hasSize(2);

        counter = 0;

        for (MBeanAttributeInfo attr : beanInfo.getAttributes()) {
            if ("ProcessDefinitions".equals(attr.getName())) {
                counter++;
                assertThat(attr.getDescription()).isEqualTo("List of Process definitions");
                assertThat(attr.getType()).isEqualTo("java.util.List");
            } else if ("Deployments".equals(attr.getName())) {
                counter++;
                assertThat(attr.getDescription()).isEqualTo("List of deployed Processes");
                assertThat(attr.getType()).isEqualTo("java.util.List");
            }

        }
        assertThat(counter).isEqualTo(2);
    }

}
