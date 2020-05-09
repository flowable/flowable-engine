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
package org.flowable.standalone.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.flowable.common.engine.api.history.HistoricData;
import org.flowable.engine.history.ProcessInstanceHistoryLog;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.test.ResourceFlowableTestCase;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.types.EntityManagerSession;
import org.flowable.variable.service.impl.types.EntityManagerSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * @author Daisuke Yoshimoto
 */
@Tag("jpa")
public class HistoricJPAVariableTest extends ResourceFlowableTestCase {

    private EntityManagerFactory entityManagerFactory;

    private FieldAccessJPAEntity simpleEntityFieldAccess;

    protected String processInstanceId;

    public HistoricJPAVariableTest() {
        super("org/flowable/standalone/jpa/flowable.cfg.xml");
    }

    @BeforeEach
    protected void setUp() {
        entityManagerFactory = ((EntityManagerSessionFactory) processEngineConfiguration.getSessionFactories().get(EntityManagerSession.class))
                .getEntityManagerFactory();
    }

    public void setupJPAEntities() {
        EntityManager manager = entityManagerFactory.createEntityManager();
        manager.getTransaction().begin();

        // Simple test data
        simpleEntityFieldAccess = new FieldAccessJPAEntity();
        simpleEntityFieldAccess.setId(1L);
        simpleEntityFieldAccess.setValue("value1");
        manager.persist(simpleEntityFieldAccess);

        manager.flush();
        manager.getTransaction().commit();
        manager.close();
    }

    @Test
    @Deployment
    public void testGetJPAEntityAsHistoricVariable() {
        setupJPAEntities();
        // -----------------------------------------------------------------------------
        // Simple test, Start process with JPA entities as variables
        // -----------------------------------------------------------------------------
        Map<String, Object> variables = new HashMap<>();
        variables.put("simpleEntityFieldAccess", simpleEntityFieldAccess);

        // Start the process with the JPA-entities as variables. They will be stored in the DB.
        this.processInstanceId = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables).getId();

        for (org.flowable.task.api.Task task : taskService.createTaskQuery().includeTaskLocalVariables().list()) {
            taskService.complete(task.getId());
        }

        // Get JPAEntity Variable by HistoricVariableInstanceQuery
        HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).variableName("simpleEntityFieldAccess").singleResult();

        Object value = historicVariableInstance.getValue();
        assertThat(value).isInstanceOf(FieldAccessJPAEntity.class);
        assertThat(simpleEntityFieldAccess.getValue()).isEqualTo(((FieldAccessJPAEntity) value).getValue());
    }

    @Test
    @Deployment
    public void testGetJPAEntityAsHistoricLog() {
        setupJPAEntities();
        // -----------------------------------------------------------------------------
        // Simple test, Start process with JPA entities as variables
        // -----------------------------------------------------------------------------
        Map<String, Object> variables = new HashMap<>();
        variables.put("simpleEntityFieldAccess", simpleEntityFieldAccess);

        // Start the process with the JPA-entities as variables. They will be stored in the DB.
        this.processInstanceId = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables).getId();

        // Finish tasks
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().includeTaskLocalVariables().list()) {
            taskService.complete(task.getId());
        }

        // Get JPAEntity Variable by ProcessInstanceHistoryLogQuery
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                .includeVariables()
                .singleResult();
        List<HistoricData> events = log.getHistoricData();

        for (HistoricData event : events) {
            Object value = ((HistoricVariableInstanceEntity) event).getValue();
            assertThat(value).isInstanceOf(FieldAccessJPAEntity.class);
            assertThat(simpleEntityFieldAccess.getValue()).isEqualTo(((FieldAccessJPAEntity) value).getValue());
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/standalone/jpa/HistoricJPAVariableTest.testGetJPAEntityAsHistoricLog.bpmn20.xml" })
    public void testGetJPAUpdateEntityAsHistoricLog() {
        setupJPAEntities();
        // -----------------------------------------------------------------------------
        // Simple test, Start process with JPA entities as variables
        // -----------------------------------------------------------------------------
        Map<String, Object> variables = new HashMap<>();
        variables.put("simpleEntityFieldAccess", simpleEntityFieldAccess);

        // Start the process with the JPA-entities as variables. They will be stored in the DB.
        this.processInstanceId = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables).getId();

        // Finish tasks
        for (org.flowable.task.api.Task task : taskService.createTaskQuery().includeProcessVariables().list()) {
            taskService.setVariable(task.getId(), "simpleEntityFieldAccess", simpleEntityFieldAccess);
            taskService.complete(task.getId());
        }

        // Get JPAEntity Variable by ProcessInstanceHistoryLogQuery
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                .includeVariableUpdates()
                .singleResult();
        List<HistoricData> events = log.getHistoricData();

        for (HistoricData event : events) {
            Object value = ((HistoricDetailVariableInstanceUpdateEntity) event).getValue();
            assertThat(value).isInstanceOf(FieldAccessJPAEntity.class);
            assertThat(simpleEntityFieldAccess.getValue()).isEqualTo(((FieldAccessJPAEntity) value).getValue());
        }
    }
}
