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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class ProcessDefinitionQueryTest extends PluggableFlowableTestCase {

    private String deploymentOneId;
    private String deploymentTwoId;

    @BeforeEach
    protected void setUp() throws Exception {
        deploymentOneId = repositoryService.createDeployment().name("org/flowable/engine/test/repository/one.bpmn20.xml").addClasspathResource("org/flowable/engine/test/repository/one.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/repository/two.bpmn20.xml").deploy().getId();

        deploymentTwoId = repositoryService.createDeployment().name("org/flowable/engine/test/repository/one.bpmn20.xml").addClasspathResource("org/flowable/engine/test/repository/one.bpmn20.xml")
                .deploy().getId();

    }

    @AfterEach
    protected void tearDown() throws Exception {
        repositoryService.deleteDeployment(deploymentOneId, true);
        repositoryService.deleteDeployment(deploymentTwoId, true);
    }

    @Test
    public void testProcessDefinitionProperties() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionName().asc().orderByProcessDefinitionVersion().asc()
                .orderByProcessDefinitionCategory().asc().list();

        assertThat(processDefinitions)
                .extracting(ProcessDefinition::getKey, ProcessDefinition::getName, ProcessDefinition::getCategory)
                .containsExactly(
                        tuple("one", "One", "Examples"),
                        tuple("one", "One", "Examples"),
                        tuple("two", "Two", "Examples2")
                );
        assertThat(processDefinitions.get(0).getId()).startsWith("one:1");
        assertThat(processDefinitions.get(1).getId()).startsWith("one:2");
        assertThat(processDefinitions.get(2).getId()).startsWith("two:1");
    }

    @Test
    public void testQueryByDeploymentId() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentOneId);
        verifyQueryResults(query, 2);
    }

    @Test
    public void testQueryByInvalidDeploymentId() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().deploymentId("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> repositoryService.createProcessDefinitionQuery().deploymentId(null))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByParentDeploymentId() {
        Deployment deployment1 = repositoryService.createDeployment()
                .parentDeploymentId("parent1")
                .addClasspathResource("org/flowable/engine/test/repository/one.bpmn20.xml")
                .deploy();

        Deployment deployment2 = repositoryService.createDeployment()
                .parentDeploymentId("parent2")
                .addClasspathResource("org/flowable/engine/test/repository/one.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/repository/two.bpmn20.xml")
                .deploy();

        try {

            assertThat(repositoryService.createProcessDefinitionQuery().parentDeploymentId("parent1").list())
                    .extracting(ProcessDefinition::getKey, ProcessDefinition::getDeploymentId)
                    .containsExactlyInAnyOrder(
                            tuple("one", deployment1.getId())
                    );
            assertThat(repositoryService.createProcessDefinitionQuery().parentDeploymentId("parent1").count()).isEqualTo(1);

            assertThat(repositoryService.createProcessDefinitionQuery().parentDeploymentId("parent2").list())
                    .extracting(ProcessDefinition::getKey, ProcessDefinition::getDeploymentId)
                    .containsExactlyInAnyOrder(
                            tuple("one", deployment2.getId()),
                            tuple("two", deployment2.getId())
                    );
            assertThat(repositoryService.createProcessDefinitionQuery().parentDeploymentId("parent2").count()).isEqualTo(2);

            assertThat(repositoryService.createProcessDefinitionQuery().parentDeploymentId("unknown").list()).isEmpty();
            assertThat(repositoryService.createProcessDefinitionQuery().parentDeploymentId("unknown").count()).isEqualTo(0);
        } finally {
            repositoryService.deleteDeployment(deployment1.getId(), true);
            repositoryService.deleteDeployment(deployment2.getId(), true);
        }
    }

    @Test
    public void testQueryByName() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionName("Two");
        verifyQueryResults(query, 1);

        query = repositoryService.createProcessDefinitionQuery().processDefinitionName("One");
        verifyQueryResults(query, 2);
    }

    @Test
    public void testQueryByInvalidName() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionName("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> repositoryService.createProcessDefinitionQuery().processDefinitionName(null))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByNameLike() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionNameLike("%w%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidNameLike() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionNameLike("%invalid%");
        verifyQueryResults(query, 0);
    }

    @Test
    public void testQueryByNameLikeIgnoreCase() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionNameLikeIgnoreCase("%two%");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidNameLikeIgnoreCase() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionNameLikeIgnoreCase("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> repositoryService.createProcessDefinitionQuery().processDefinitionNameLikeIgnoreCase(null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("nameLikeIgnoreCase is null");
    }

    @Test
    public void testQueryByKey() {
        // process one
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one");
        verifyQueryResults(query, 2);

        // process two
        query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("two");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByInvalidKey() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("invalid");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> repositoryService.createProcessDefinitionQuery().processDefinitionKey(null))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByKeyLike() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKeyLike("%o%");
        verifyQueryResults(query, 3);
    }

    @Test
    public void testQueryByInvalidKeyLike() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKeyLike("%invalid%");
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> repositoryService.createProcessDefinitionQuery().processDefinitionKeyLike(null))
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByCategory() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionCategory("Examples");
        verifyQueryResults(query, 2);
    }

    @Test
    public void testQueryByCategoryLike() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionCategoryLike("%Example%");
        verifyQueryResults(query, 3);

        query = repositoryService.createProcessDefinitionQuery().processDefinitionCategoryLike("%amples2");
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQueryByVersion() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(2);
        verifyQueryResults(query, 1);

        query = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(1);
        verifyQueryResults(query, 2);
    }

    @Test
    public void testQueryByInvalidVersion() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionVersion(3);
        verifyQueryResults(query, 0);

        assertThatThrownBy(() -> repositoryService.createProcessDefinitionQuery().processDefinitionVersion(-1).list())
                .isInstanceOf(FlowableIllegalArgumentException.class);

        assertThatThrownBy(() -> repositoryService.createProcessDefinitionQuery().processDefinitionVersion(null).list())
                .isInstanceOf(FlowableIllegalArgumentException.class);
    }

    @Test
    public void testQueryByKeyAndVersion() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").processDefinitionVersion(1);
        verifyQueryResults(query, 1);

        query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").processDefinitionVersion(2);
        verifyQueryResults(query, 1);

        query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").processDefinitionVersion(3);
        verifyQueryResults(query, 0);
    }

    @Test
    public void testQueryByLatest() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().latestVersion();
        verifyQueryResults(query, 2);

        query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").latestVersion();
        verifyQueryResults(query, 1);

        query = repositoryService.createProcessDefinitionQuery().processDefinitionKey("two").latestVersion();
        verifyQueryResults(query, 1);
    }

    @Test
    public void testQuerySorting() {

        // asc

        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionId().asc();
        verifyQueryResults(query, 3);

        query = repositoryService.createProcessDefinitionQuery().orderByDeploymentId().asc();
        verifyQueryResults(query, 3);

        query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionKey().asc();
        verifyQueryResults(query, 3);

        query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionVersion().asc();
        verifyQueryResults(query, 3);

        // desc

        query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionId().desc();
        verifyQueryResults(query, 3);

        query = repositoryService.createProcessDefinitionQuery().orderByDeploymentId().desc();
        verifyQueryResults(query, 3);

        query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionKey().desc();
        verifyQueryResults(query, 3);

        query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionVersion().desc();
        verifyQueryResults(query, 3);

        // Typical use case
        query = repositoryService.createProcessDefinitionQuery().orderByProcessDefinitionKey().asc().orderByProcessDefinitionVersion().desc();
        List<ProcessDefinition> processDefinitions = query.list();
        assertThat(processDefinitions)
                .extracting(ProcessDefinition::getKey, ProcessDefinition::getVersion)
                .containsExactly(
                        tuple("one", 2),
                        tuple("one", 1),
                        tuple("two", 1)
                );
    }

    private void verifyQueryResults(ProcessDefinitionQuery query, int countExpected) {
        assertThat(query.list()).hasSize(countExpected);
        assertThat(query.count()).isEqualTo(countExpected);

        if (countExpected == 1) {
            assertThat(query.singleResult()).isNotNull();
        } else if (countExpected > 1) {
            verifySingleResultFails(query);
        } else if (countExpected == 0) {
            assertThat(query.singleResult()).isNull();
        }
    }

    private void verifySingleResultFails(ProcessDefinitionQuery query) {
        assertThatThrownBy(() -> query.singleResult())
                .isInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByMessageSubscription() {
        Deployment deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/repository/processWithNewBookingMessage.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/repository/processWithNewInvoiceMessage.bpmn20.xml").deploy();

        assertThat(repositoryService.createProcessDefinitionQuery().messageEventSubscriptionName("newInvoiceMessage").count()).isEqualTo(1);

        assertThat(repositoryService.createProcessDefinitionQuery().messageEventSubscriptionName("newBookingMessage").count()).isEqualTo(1);

        assertThat(repositoryService.createProcessDefinitionQuery().messageEventSubscriptionName("bogus").count()).isZero();

        repositoryService.deleteDeployment(deployment.getId());
    }

    @Test
    public void testNativeQuery() {
        assertThat(managementService.getTableName(ProcessDefinition.class, false)).isEqualTo("ACT_RE_PROCDEF");
        assertThat(managementService.getTableName(ProcessDefinitionEntity.class, false)).isEqualTo("ACT_RE_PROCDEF");
        String tableName = managementService.getTableName(ProcessDefinition.class);
        String baseQuerySql = "SELECT * FROM " + tableName;

        assertThat(repositoryService.createNativeProcessDefinitionQuery().sql(baseQuerySql).list()).hasSize(3);

        assertThat(repositoryService.createNativeProcessDefinitionQuery().sql(baseQuerySql + " where KEY_ like #{key}").parameter("key", "%o%").list()).hasSize(3);

        assertThat(repositoryService.createNativeProcessDefinitionQuery().sql(baseQuerySql + " where NAME_ = #{name}").parameter("name", "One").list()).hasSize(2);

        // paging
        assertThat(repositoryService.createNativeProcessDefinitionQuery().sql(baseQuerySql).listPage(0, 2)).hasSize(2);
        assertThat(repositoryService.createNativeProcessDefinitionQuery().sql(baseQuerySql).listPage(1, 3)).hasSize(2);
    }

    @Test
    public void testQueryByProcessDefinitionIds() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        Set<String> ids = new HashSet<>();
        for (ProcessDefinition processDefinition : processDefinitions) {
            ids.add(processDefinition.getId());
        }

        List<ProcessDefinition> queryResults = repositoryService.createProcessDefinitionQuery().processDefinitionIds(ids).list();
        assertThat(ids).hasSameSizeAs(queryResults);
        for (ProcessDefinition processDefinition : queryResults) {
            assertThat(ids).contains(processDefinition.getId());
        }
    }


    @Test
    public void testLocalizeProcessDefinition() {
        Deployment deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/repository/LocalizedProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/repository/LocalizedProcess.bpmn20.xml").deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("localizedProcess")
                .singleResult();

        assertThat(processDefinition.getName()).isEqualTo("A localized process");
        assertThat(processDefinition.getDescription()).isEqualTo("This a process that can be localized");

        processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("localizedProcess")
                .locale("es")
                .singleResult();

        assertThat(processDefinition.getName()).isEqualTo("Nombre del proceso");
        assertThat(processDefinition.getDescription()).isEqualTo("Descripción del proceso");

        ObjectNode infoNode = dynamicBpmnService.getProcessDefinitionInfo(processDefinition.getId());
        dynamicBpmnService.changeLocalizationName("en-GB", "localizedProcess", "The process name in 'en-GB'", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-GB", "localizedProcess", "The process description in 'en-GB'", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processDefinition.getId(), infoNode);

        processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("localizedProcess")
                .locale("en-GB")
                .singleResult();

        assertThat(processDefinition.getName()).isEqualTo("The process name in 'en-GB'");
        assertThat(processDefinition.getDescription()).isEqualTo("The process description in 'en-GB'");

        repositoryService.deleteDeployment(deployment.getId());
    }
}
