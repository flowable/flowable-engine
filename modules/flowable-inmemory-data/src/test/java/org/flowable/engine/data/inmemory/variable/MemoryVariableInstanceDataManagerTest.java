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
package org.flowable.engine.data.inmemory.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.flowable.engine.data.inmemory.MemoryDataManagerFlowableTestCase;
import org.flowable.engine.data.inmemory.impl.variable.MemoryVariableInstanceDataManager;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.runtime.VariableInstanceQuery;
import org.flowable.variable.service.InternalVariableInstanceQuery;
import org.flowable.variable.service.impl.InternalVariableInstanceQueryImpl;
import org.flowable.variable.service.impl.VariableInstanceQueryImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryVariableInstanceDataManagerTest extends MemoryDataManagerFlowableTestCase {

    private Map<String, Object> variables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("string", "value");
        variables.put("long", 42l);
        variables.put("int", 1);
        variables.put("short", (short) 1);
        variables.put("double", 0.42d);
        variables.put("boolean", true);
        return variables;
    }

    @Test
    public void testFindVariables() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        MemoryVariableInstanceDataManager variableManager = getVariableInstanceDataManager();
        try {
            Map<String, Object> variables = variables();
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", variables);
            assertThat(variableManager.findByProcessId(instance.getId())).hasSize(variables.size());
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryVariables() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();
        try {
            Map<String, Object> variables = variables();
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", variables);

            VariableInstanceQueryImpl query = Mockito.spy(query());
            query.processInstanceId(instance.getId()).variableName("string").list();

            assertQueryMethods(VariableInstanceQueryImpl.class, query,
                            // not part of queries
                            "getId", "isExcludeVariableInitialization",
                            // unused?
                            "getActivityInstanceId");
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryVariablesName() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();
        try {
            Map<String, Object> variables = variables();
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", variables);
            for (String name : variables.keySet()) {
                VariableInstanceQuery query = query().processInstanceId(instance.getId());
                query.variableName(name);
                assertThat(query.list()).hasSize(1);
            }
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryVariablesNameLike() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();
        try {
            Map<String, Object> variables = variables();
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", variables);
            assertThat(query().processInstanceId(instance.getId()).variableNameLike("%tring").list()).hasSize(1);
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryVariableValueEquals() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        try {
            Map<String, Object> variables = variables();
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", variables);

            for (Entry<String, Object> entry : variables.entrySet()) {
                VariableInstanceQuery query = query().processInstanceId(instance.getId()).variableValueEquals(entry.getKey(), entry.getValue());
                assertThat(query.list()).hasSize(1);
            }
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testInternalQueryVariableName() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        try {
            Map<String, Object> variables = variables();
            processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", variables);

            for (Entry<String, Object> entry : variables.entrySet()) {
                InternalVariableInstanceQuery query = new InternalVariableInstanceQueryImpl(getVariableInstanceDataManager()).name(entry.getKey());
                assertThat(query.list()).hasSize(1);
            }
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testNativeQueryThrows() {
        MemoryVariableInstanceDataManager variableManager = getVariableInstanceDataManager();
        try {
            assertThatThrownBy(() -> variableManager.findVariableInstanceCountByNativeQuery(new HashMap<>())).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> variableManager.findVariableInstancesByNativeQuery(new HashMap<>())).isInstanceOf(IllegalStateException.class);
        } finally {
            processEngine.close();
        }
    }

    private VariableInstanceQueryImpl query() {
        return (VariableInstanceQueryImpl) processEngine.getRuntimeService().createVariableInstanceQuery();
    }

}
