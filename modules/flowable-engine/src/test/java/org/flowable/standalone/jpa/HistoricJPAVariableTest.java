package org.flowable.standalone.jpa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.history.HistoricData;
import org.flowable.engine.history.HistoricVariableInstance;
import org.flowable.engine.history.ProcessInstanceHistoryLog;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.engine.impl.test.AbstractFlowableTestCase;
import org.flowable.engine.impl.variable.EntityManagerSession;
import org.flowable.engine.impl.variable.EntityManagerSessionFactory;
import org.flowable.engine.task.Task;
import org.flowable.engine.test.Deployment;

/**
 * @author Daisuke Yoshimoto
 */
public class HistoricJPAVariableTest extends AbstractFlowableTestCase {

    protected static ProcessEngine cachedProcessEngine;

    private static EntityManagerFactory entityManagerFactory;

    private static FieldAccessJPAEntity simpleEntityFieldAccess;
    private static boolean entitiesInitialized;

    protected String processInstanceId;

    @Override
    protected void initializeProcessEngine() {
        if (cachedProcessEngine == null) {
            ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                    .createProcessEngineConfigurationFromResource("org/flowable/standalone/jpa/flowable.cfg.xml");

            cachedProcessEngine = processEngineConfiguration.buildProcessEngine();

            EntityManagerSessionFactory entityManagerSessionFactory = (EntityManagerSessionFactory) processEngineConfiguration
                    .getSessionFactories()
                    .get(EntityManagerSession.class);

            entityManagerFactory = entityManagerSessionFactory.getEntityManagerFactory();
        }
        processEngine = cachedProcessEngine;
    }

    public void setupJPAEntities() {
        if (!entitiesInitialized) {
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
            entitiesInitialized = true;
        }
    }

    @Deployment
    public void testGetJPAEntityAsHistoricVariable() {
        setupJPAEntities();
        // -----------------------------------------------------------------------------
        // Simple test, Start process with JPA entities as variables
        // -----------------------------------------------------------------------------
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("simpleEntityFieldAccess", simpleEntityFieldAccess);

        // Start the process with the JPA-entities as variables. They will be stored in the DB.
        this.processInstanceId = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables).getId();

        for (Task task : taskService.createTaskQuery().includeTaskLocalVariables().list()) {
            taskService.complete(task.getId());
        }

        // Get JPAEntity Variable by HistoricVariableInstanceQuery
        HistoricVariableInstance historicVariableInstance = historyService.createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId).variableName("simpleEntityFieldAccess").singleResult();

        Object value = historicVariableInstance.getValue();
        assertTrue(value instanceof FieldAccessJPAEntity);
        assertEquals(((FieldAccessJPAEntity) value).getValue(), simpleEntityFieldAccess.getValue());
    }

    @Deployment
    public void testGetJPAEntityAsHistoricLog() {
        setupJPAEntities();
        // -----------------------------------------------------------------------------
        // Simple test, Start process with JPA entities as variables
        // -----------------------------------------------------------------------------
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("simpleEntityFieldAccess", simpleEntityFieldAccess);

        // Start the process with the JPA-entities as variables. They will be stored in the DB.
        this.processInstanceId = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables).getId();

        // Finish tasks
        for (Task task : taskService.createTaskQuery().includeTaskLocalVariables().list()) {
            taskService.complete(task.getId());
        }

        // Get JPAEntity Variable by ProcessInstanceHistoryLogQuery
        ProcessInstanceHistoryLog log = historyService.createProcessInstanceHistoryLogQuery(processInstanceId)
                .includeVariables()
                .singleResult();
        List<HistoricData> events = log.getHistoricData();

        for (HistoricData event : events) {
            Object value = ((HistoricVariableInstanceEntity) event).getValue();
            assertTrue(value instanceof FieldAccessJPAEntity);
            assertEquals(((FieldAccessJPAEntity) value).getValue(), simpleEntityFieldAccess.getValue());
        }
    }

    @Deployment(resources = { "org/flowable/standalone/jpa/HistoricJPAVariableTest.testGetJPAEntityAsHistoricLog.bpmn20.xml" })
    public void testGetJPAUpdateEntityAsHistoricLog() {
        setupJPAEntities();
        // -----------------------------------------------------------------------------
        // Simple test, Start process with JPA entities as variables
        // -----------------------------------------------------------------------------
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("simpleEntityFieldAccess", simpleEntityFieldAccess);

        // Start the process with the JPA-entities as variables. They will be stored in the DB.
        this.processInstanceId = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables).getId();

        // Finish tasks
        for (Task task : taskService.createTaskQuery().includeProcessVariables().list()) {
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
            assertTrue(value instanceof FieldAccessJPAEntity);
            assertEquals(((FieldAccessJPAEntity) value).getValue(), simpleEntityFieldAccess.getValue());
        }
    }
}
