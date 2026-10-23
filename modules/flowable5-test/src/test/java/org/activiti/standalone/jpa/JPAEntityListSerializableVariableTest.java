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
package org.activiti.standalone.jpa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.activiti.engine.impl.test.AbstractFlowableTestCase;
import org.activiti.engine.impl.variable.EntityManagerSession;
import org.activiti.engine.impl.variable.EntityManagerSessionFactory;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;

/**
 * Tests for list variables that switch between the JPA entity list type and other list values (an empty list or a list of strings). Those other
 * list values can only be stored as serializable variables, so these tests require serializable variables to be enabled.
 *
 * @author Frederik Heremans
 */
public class JPAEntityListSerializableVariableTest extends AbstractFlowableTestCase {

    protected static ProcessEngine cachedProcessEngine;

    private static FieldAccessJPAEntity simpleEntityFieldAccess;
    private static PropertyAccessJPAEntity simpleEntityPropertyAccess;

    private static boolean entitiesInitialized;

    private static EntityManagerFactory entityManagerFactory;

    @Override
    protected void initializeProcessEngine() {
        if (cachedProcessEngine == null) {
            ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
                    .createProcessEngineConfigurationFromResource("org/activiti/standalone/jpa/serializable.flowable.cfg.xml");

            cachedProcessEngine = processEngineConfiguration.buildProcessEngine();

            org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl activiti5ProcessEngineConfig = (org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl) ((ProcessEngineConfigurationImpl) cachedProcessEngine
                    .getProcessEngineConfiguration()).getFlowable5CompatibilityHandler().getRawProcessConfiguration();

            EntityManagerSessionFactory entityManagerSessionFactory = (EntityManagerSessionFactory) activiti5ProcessEngineConfig
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

            simpleEntityFieldAccess = new FieldAccessJPAEntity();
            simpleEntityFieldAccess.setId(1L);
            simpleEntityFieldAccess.setValue("value1");
            manager.persist(simpleEntityFieldAccess);

            simpleEntityPropertyAccess = new PropertyAccessJPAEntity();
            simpleEntityPropertyAccess.setId(1L);
            simpleEntityPropertyAccess.setValue("value2");
            manager.persist(simpleEntityPropertyAccess);

            manager.flush();
            manager.getTransaction().commit();
            manager.close();

            entitiesInitialized = true;
        }
    }

    @Deployment(resources = {
            "org/activiti/standalone/jpa/JPAVariableTest.testStoreJPAEntityAsVariable.bpmn20.xml"
    })
    public void testStoreJPAEntityListAsVariableEdgeCases() {
        setupJPAEntities();

        // Test using mixed JPA-entities which are not serializable, should not be picked up by JPA list type en therefor fail
        // due to serialization error
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("simpleEntityFieldAccess", Arrays.asList(simpleEntityFieldAccess, simpleEntityPropertyAccess));

        try {
            runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables);
            fail("Exception expected");
        } catch (FlowableException ae) {
            // Expected
        }

        // Test updating value to an empty list and back
        variables = new HashMap<String, Object>();
        variables.put("list", Arrays.asList(simpleEntityFieldAccess, simpleEntityFieldAccess));

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("JPAVariableProcess", variables);

        runtimeService.setVariable(processInstance.getId(), "list", new ArrayList<String>());
        assertEquals(0L, ((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).size());

        runtimeService.setVariable(processInstance.getId(), "list", Arrays.asList(simpleEntityFieldAccess, simpleEntityFieldAccess));
        assertEquals(2L, ((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).size());
        assertTrue(((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).get(0) instanceof FieldAccessJPAEntity);

        // Test updating to list of Strings
        runtimeService.setVariable(processInstance.getId(), "list", Arrays.asList("TEST", "TESTING"));
        assertEquals(2L, ((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).size());
        assertTrue(((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).get(0) instanceof String);

        runtimeService.setVariable(processInstance.getId(), "list", Arrays.asList(simpleEntityFieldAccess, simpleEntityFieldAccess));
        assertEquals(2L, ((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).size());
        assertTrue(((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).get(0) instanceof FieldAccessJPAEntity);

        // Test updating to null
        runtimeService.setVariable(processInstance.getId(), "list", null);
        assertNull(runtimeService.getVariable(processInstance.getId(), "list"));

        runtimeService.setVariable(processInstance.getId(), "list", Arrays.asList(simpleEntityFieldAccess, simpleEntityFieldAccess));
        assertEquals(2L, ((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).size());
        assertTrue(((List<?>) runtimeService.getVariable(processInstance.getId(), "list")).get(0) instanceof FieldAccessJPAEntity);
    }

}
