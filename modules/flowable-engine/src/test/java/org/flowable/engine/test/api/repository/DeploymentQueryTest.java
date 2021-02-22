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
import static org.assertj.core.api.Assertions.tuple;

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
        assertThat(query.list()).hasSize(2);
        assertThat(query.count()).isEqualTo(2);

        assertThatThrownBy(() -> query.singleResult())
                .isInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByDeploymentId() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentId(deploymentOneId);
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidDeploymentId() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentId("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> repositoryService.createDeploymentQuery().deploymentId(null))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByName() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentName("org/flowable/engine/test/repository/two.bpmn20.xml");
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidName() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentName("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> repositoryService.createDeploymentQuery().deploymentName(null))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNameLike() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentNameLike("%flowable%");
        assertThat(query.list()).hasSize(2);
        assertThat(query.count()).isEqualTo(2);

        assertThatThrownBy(() -> query.singleResult())
                .isInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByInvalidNameLike() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentNameLike("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();

        assertThatThrownBy(() -> repositoryService.createDeploymentQuery().deploymentNameLike(null))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNameAndCategory() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentCategory("testCategory").deploymentNameLike("%flowable%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult()).isNotNull();
    }

    @Test
    public void testQueryByProcessDefinitionKey() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().processDefinitionKey("one");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        assertThat(query.singleResult()).isNotNull();
    }

    @Test
    public void testQueryByProcessDefinitionKeyLike() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().processDefinitionKeyLike("%o%");
        assertThat(query.list()).hasSize(2);
        assertThat(query.count()).isEqualTo(2);
    }
    
    @Test
    public void testQueryByProcessDefinitionKeyLikeMultipleProcessDefinitions() {
        String deploymentId = repositoryService.createDeployment().name("org/flowable/engine/test/repository/one.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/repository/one.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/repository/two.bpmn20.xml")
                .deploy().getId();

        DeploymentQuery query = repositoryService.createDeploymentQuery().deploymentId(deploymentId).processDefinitionKeyLike("%o%");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
        
        repositoryService.deleteDeployment(deploymentId, true);
    }
    
    @Test
    public void testQueryByInvalidProcessDefinitionKeyLike() {
        DeploymentQuery query = repositoryService.createDeploymentQuery().processDefinitionKeyLike("invalid");
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testVerifyDeploymentProperties() {
        List<Deployment> deployments = repositoryService.createDeploymentQuery().orderByDeploymentName().asc().list();
        assertThat(deployments)
                .extracting(Deployment::getName, Deployment::getId)
                .containsExactly(
                        tuple("org/flowable/engine/test/repository/one.bpmn20.xml", deploymentOneId),
                        tuple("org/flowable/engine/test/repository/two.bpmn20.xml", deploymentTwoId)
                );

        deployments = repositoryService.createDeploymentQuery().deploymentNameLike("%one%").orderByDeploymentName().asc().list();
        assertThat(deployments)
                .extracting(Deployment::getName, Deployment::getId)
                .containsExactly(
                        tuple("org/flowable/engine/test/repository/one.bpmn20.xml", deploymentOneId)
                );

        assertThat(repositoryService.createDeploymentQuery().orderByDeploymentId().asc().list()).hasSize(2);

        assertThat(repositoryService.createDeploymentQuery().orderByDeploymentTime().asc().list()).hasSize(2);

    }

    @Test
    public void testNativeQuery() {
        assertThat(managementService.getTableName(Deployment.class, false)).isEqualTo("ACT_RE_DEPLOYMENT");
        assertThat(managementService.getTableName(DeploymentEntity.class, false)).isEqualTo("ACT_RE_DEPLOYMENT");
        String tableName = managementService.getTableName(Deployment.class);
        String baseQuerySql = "SELECT * FROM " + tableName;

        assertThat(repositoryService.createNativeDeploymentQuery().sql(baseQuerySql).list()).hasSize(2);

        assertThat(repositoryService.createNativeDeploymentQuery().sql(baseQuerySql + " where NAME_ = #{name}").parameter("name", "org/flowable/engine/test/repository/one.bpmn20.xml").list()).hasSize(1);

        // paging
        assertThat(repositoryService.createNativeDeploymentQuery().sql(baseQuerySql).listPage(0, 2)).hasSize(2);
        assertThat(repositoryService.createNativeDeploymentQuery().sql(baseQuerySql).listPage(1, 3)).hasSize(1);
    }

}
