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

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Tom Baeyens
 */
public class DeploymentQueryTest extends PluggableFlowableTestCase {

    private String deploymentOneId;

    private String deploymentTwoId;

    @BeforeEach
    protected void setUp() throws Exception {
        deploymentOneId = repositoryService.createDeployment().name("org/flowable/engine/test/repository/one.bpmn20.xml").category("testCategory")
                .addClasspathResource("org/flowable/engine/test/repository/one.bpmn20.xml").deploy().getId();

        deploymentTwoId = repositoryService.createDeployment().name("org/flowable/engine/test/repository/two.bpmn20.xml").addClasspathResource("org/flowable/engine/test/repository/two.bpmn20.xml")
                .deploy().getId();

    }

    @AfterEach
    protected void tearDown() throws Exception {
        repositoryService.deleteDeployment(deploymentOneId, true);
        repositoryService.deleteDeployment(deploymentTwoId, true);
    }

    @Test
    public void testQueryNoCriteria() {
        DeploymentQuery query = repositoryService.createDeploymentQuery();
        assertEquals(2, query.list().size());
        assertEquals(2, query.count());

        try {
            query.singleResult();
            fail();
        } catch (FlowableException e) {
        }
    }

    @Test
    public void testQueryByDeploymentId() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentId(deploymentOneId);
        assertNotNull(query.singleResult());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());
    }

    @Test
    public void testQueryByInvalidDeploymentId() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentId("invalid");
        assertNull(query.singleResult());
        assertEquals(0, query.list().size());
        assertEquals(0, query.count());

        try {
            repositoryService.createDeploymentQuery().deploymentId(null);
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    @Test
    public void testQueryByName() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentName("org/flowable/engine/test/repository/two.bpmn20.xml");
        assertNotNull(query.singleResult());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());
    }

    @Test
    public void testQueryByInvalidName() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentName("invalid");
        assertNull(query.singleResult());
        assertEquals(0, query.list().size());
        assertEquals(0, query.count());

        try {
            repositoryService.createDeploymentQuery().deploymentName(null);
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    @Test
    public void testQueryByNameLike() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentNameLike("%flowable%");
        assertEquals(2, query.list().size());
        assertEquals(2, query.count());

        try {
            query.singleResult();
            fail();
        } catch (FlowableException e) {
        }
    }

    @Test
    public void testQueryByInvalidNameLike() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentNameLike("invalid");
        assertNull(query.singleResult());
        assertEquals(0, query.list().size());
        assertEquals(0, query.count());

        try {
            repositoryService.createDeploymentQuery().deploymentNameLike(null);
            fail();
        } catch (FlowableIllegalArgumentException e) {
        }
    }

    @Test
    public void testQueryByNameAndCategory() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentCategory("testCategory").deploymentNameLike("%flowable%");
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());
        assertNotNull(query.singleResult());
    }

    @Test
    public void testQueryByProcessDefinitionKey() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().processDefinitionKey("one");
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());
        assertNotNull(query.singleResult());
    }

    @Test
    public void testQueryByProcessDefinitionKeyLike() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().processDefinitionKeyLike("%o%");
        assertEquals(2, query.list().size());
        assertEquals(2, query.count());
    }
    
    @Test
    public void testQueryByProcessDefinitionKeyLikeMultipleProcessDefinitions() {
        String deploymentId = repositoryService.createDeployment().name("org/flowable/engine/test/repository/one.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/repository/one.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/repository/two.bpmn20.xml")
                .deploy().getId();

        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentId(deploymentId).processDefinitionKeyLike("%o%");
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());
        
        repositoryService.deleteDeployment(deploymentId, true);
    }
    
    @Test
    public void testQueryByInvalidProcessDefinitionKeyLike() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().processDefinitionKeyLike("invalid");
        assertEquals(0, query.list().size());
        assertEquals(0, query.count());
    }

    @Test
    public void testVerifyDeploymentProperties() {
        List<Deployment> deployments = repositoryService.createDeploymentQuery().orderByDeploymentName().asc().list();

        Deployment deploymentOne = deployments.get(0);
        assertEquals("org/flowable/engine/test/repository/one.bpmn20.xml", deploymentOne.getName());
        assertEquals(deploymentOneId, deploymentOne.getId());

        Deployment deploymentTwo = deployments.get(1);
        assertEquals("org/flowable/engine/test/repository/two.bpmn20.xml", deploymentTwo.getName());
        assertEquals(deploymentTwoId, deploymentTwo.getId());

        deployments = repositoryService.createDeploymentQuery().deploymentNameLike("%one%").orderByDeploymentName().asc().list();

        assertEquals("org/flowable/engine/test/repository/one.bpmn20.xml", deployments.get(0).getName());
        assertEquals(1, deployments.size());

        assertEquals(2, repositoryService.createDeploymentQuery().orderByDeploymentId().asc().list().size());

        assertEquals(2, repositoryService.createDeploymentQuery().orderByDeploymenTime().asc().list().size());

    }

    @Test
    public void testNativeQuery() {
        assertEquals("ACT_RE_DEPLOYMENT", managementService.getTableName(Deployment.class));
        assertEquals("ACT_RE_DEPLOYMENT", managementService.getTableName(DeploymentEntity.class));
        String tableName = managementService.getTableName(Deployment.class);
        String baseQuerySql = "SELECT * FROM " + tableName;

        assertEquals(2, repositoryService.createNativeDeploymentQuery().sql(baseQuerySql).list().size());

        assertEquals(1, repositoryService.createNativeDeploymentQuery().sql(baseQuerySql + " where NAME_ = #{name}").parameter("name", "org/flowable/engine/test/repository/one.bpmn20.xml").list().size());

        // paging
        assertEquals(2, repositoryService.createNativeDeploymentQuery().sql(baseQuerySql).listPage(0, 2).size());
        assertEquals(1, repositoryService.createNativeDeploymentQuery().sql(baseQuerySql).listPage(1, 3).size());
    }

}
