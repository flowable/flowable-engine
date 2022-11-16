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
package org.flowable.engine.data.inmemory.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.data.inmemory.MemoryDataManagerFlowableTestCase;
import org.flowable.engine.data.inmemory.impl.execution.MemoryExecutionDataManager;
import org.flowable.engine.impl.ExecutionQueryImpl;
import org.flowable.engine.impl.ProcessInstanceQueryImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryExecutionDataManagerTest extends MemoryDataManagerFlowableTestCase {

    @Test
    public void testMemoryExecutionDataManager() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/simpleExecution.bpmn20.xml").deploy();

        MemoryExecutionDataManager executionManager = getExecutionDataManager();

        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("simpleExecution");
            assertThat(executionManager.findById(instance.getId())).isNull();
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testExecutionInsertedDeleted() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        MemoryExecutionDataManager executionManager = getExecutionDataManager();
        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution");
            assertThat(executionManager.findById(instance.getId())).isNotNull();
            Execution execution = processEngine.getRuntimeService().createExecutionQuery().processInstanceId(instance.getProcessInstanceId())
                            .activityId("service1").singleResult();
            assertThat(execution).isNotNull();
            processEngine.getRuntimeService().trigger(execution.getId());
            assertThat(executionManager.findById(instance.getId())).isNull();
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testExecutionException() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/exceptionExecution.bpmn20.xml").deploy();

        MemoryExecutionDataManager executionManager = getExecutionDataManager();
        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("exceptionExecution");
            assertThat(executionManager.findById(instance.getId())).isNotNull();
            Execution execution = processEngine.getRuntimeService().createExecutionQuery().processInstanceId(instance.getProcessInstanceId())
                            .activityId("service1").singleResult();
            assertThat(execution).isNotNull();
            try {
                processEngine.getRuntimeService().trigger(execution.getId());
            } catch (FlowableException e) {
                // expected
            }
            // The trigger fails so we expect to still find the execution
            assertThat(executionManager.findById(instance.getId())).isNotNull();
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testFindExecution() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        MemoryExecutionDataManager executionManager = getExecutionDataManager();
        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", map("hello", "world"));

            assertThat(executionManager.findById(instance.getId())).isNotNull();
            assertThat(executionManager.findChildExecutionsByParentExecutionId(instance.getId()).size()).isEqualTo(1);
            assertThat(executionManager.findChildExecutionsByProcessInstanceId(instance.getId()).size()).isEqualTo(2);
            assertThat(executionManager.findExecutionsByParentExecutionAndActivityIds(instance.getId(), list("service1")).size()).isEqualTo(1);
            assertThat(executionManager.findExecutionsByRootProcessInstanceId(instance.getId()).size()).isEqualTo(2);
            assertThat(executionManager.findExecutionCountByQueryCriteria(new ExecutionQueryImpl().processInstanceId(instance.getId()))).isEqualTo(2);
            assertThat(executionManager.findExecutionsByQueryCriteria(new ExecutionQueryImpl().processInstanceId(instance.getId())).size()).isEqualTo(2);
            assertThat(executionManager.findProcessInstanceByQueryCriteria(new ProcessInstanceQueryImpl().processInstanceId(instance.getId()))).isNotNull();
            assertThat(executionManager.findProcessInstanceIdsByProcessDefinitionId(instance.getProcessDefinitionId()).size()).isGreaterThan(0);
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryExecution() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution");

            ExecutionQueryImpl query = Mockito.spy(query());

            assertThat(query.processInstanceId(instance.getProcessInstanceId()).activityId("service1").singleResult()).isNotNull();

            // Assert that all getters of ExecutionQueryImpl are called by the
            // Memory DataManager implementation
            assertQueryMethods(ExecutionQueryImpl.class, query,
                            // not part of queries
                            "getId", "getLocale", "isWithLocalizationFallback",
                            // not relevant outside sql
                            "getSafeInvolvedGroups",
                            // used only when businessKey set
                            "isIncludeChildExecutionsWithBusinessKeyQuery");
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryProcessInstance() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        try {
            ProcessInstance instance = processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution");

            ProcessInstanceQueryImpl query = Mockito.spy((ProcessInstanceQueryImpl) processEngine.getRuntimeService().createProcessInstanceQuery());

            assertThat(query.processInstanceId(instance.getProcessInstanceId()).singleResult()).isNotNull();

            // Assert that all getters of ExecutionQueryImpl are called by the
            // Memory DataManager implementation
            assertQueryMethods(ProcessInstanceQueryImpl.class, query,
                            // not part of queries
                            "getId", "getLocale", "isWithLocalizationFallback", "isIncludeProcessVariables", "iswithException",
                            // not relevant outside sql
                            "getSafeInvolvedGroups", "isNeedsProcessDefinitionOuterJoin",
                            // used only when businessKey set
                            "isIncludeChildExecutionsWithBusinessKeyQuery");
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testOrQueryExecution() throws InterruptedException {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        try {
            processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", map("test-date-query-1", 1));
            Thread.sleep(10); // force instances and query dates to different
                              // milliseconds (after and before are not
                              // inclusive)
            Date before2 = Date.from(Instant.now());
            processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", map("test-date-query-1", 2));

            Date before3 = Date.from(Instant.now());
            Thread.sleep(10);
            processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", map("test-date-query-1", 3));
            ExecutionQueryImpl query = query();

            // instance2 should be absent from result as it falls between the
            // dates of the or() query.
            assertThat(query.variableExists("test-date-query-1").or().startedAfter(before3).startedBefore(before2).list()).hasSize(2);
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }
    }

    @Test
    public void testQueryExecutionVariableNumericValues() {
        Deployment deployment = processEngine.getRepositoryService().createDeployment()
                        .addClasspathResource("org/flowable/engine/test/inmemory/triggerableExecution.bpmn20.xml").deploy();

        try {
            processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", map("test-variable-1", 100, "test-variable-2", 100.1));
            processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", map("test-variable-1", 100, "test-variable-2", 100.1));
            processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", map("test-variable-1", 200, "test-variable-2", 200.1));
            processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", map("test-variable-1", 200, "test-variable-2", 200.1));
            processEngine.getRuntimeService().startProcessInstanceByKey("triggerableExecution", map("test-variable-1", 300, "test-variable-2", 300.1));

            assertThat(query().variableValueEquals("test-variable-1", 100).list()).hasSize(2);
            assertThat(query().variableValueEquals("test-variable-1", 200).list()).hasSize(2);
            assertThat(query().variableValueLessThanOrEqual("test-variable-1", 200).list()).hasSize(4);

            assertThat(query().variableValueEquals("test-variable-2", 100.1).list()).hasSize(2);
            assertThat(query().variableValueEquals("test-variable-2", 200.1).list()).hasSize(2);
            assertThat(query().variableValueLessThanOrEqual("test-variable-2", 200.1).list()).hasSize(4);
        } finally {
            processEngine.getRepositoryService().deleteDeployment(deployment.getId(), true);
            processEngine.close();
        }

    }

    @Test
    public void testNativeQueryThrows() {
        MemoryExecutionDataManager executionManager = getExecutionDataManager();
        try {
            assertThatThrownBy(() -> executionManager.findExecutionCountByNativeQuery(new HashMap<>())).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> executionManager.findExecutionsByNativeQuery(new HashMap<>())).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> executionManager.findProcessInstanceByNativeQuery(new HashMap<>())).isInstanceOf(IllegalStateException.class);

        } finally {
            processEngine.close();
        }
    }

    private ExecutionQueryImpl query() {
        return (ExecutionQueryImpl) processEngine.getRuntimeService().createExecutionQuery();
    }

}
