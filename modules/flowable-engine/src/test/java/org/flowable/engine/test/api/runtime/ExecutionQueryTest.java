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
package org.flowable.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 * @author Frederik Heremans
 * @author Filip Hrisafov
 */
public class ExecutionQueryTest extends PluggableFlowableTestCase {

    private static final String CONCURRENT_PROCESS_KEY = "concurrent";
    private static final String SEQUENTIAL_PROCESS_KEY = "oneTaskProcess";
    private static final String CONCURRENT_PROCESS_NAME = "concurrentName";
    private static final String SEQUENTIAL_PROCESS_NAME = "oneTaskProcessName";
    private static final String CONCURRENT_PROCESS_CATEGORY = "org.flowable.engine.test.api.runtime.concurrent.Category";
    private static final String SEQUENTIAL_PROCESS_CATEGORY = "org.flowable.engine.test.api.runtime.Category";

    private List<String> concurrentProcessInstanceIds;
    private List<String> sequentialProcessInstanceIds;

    @BeforeEach
    protected void setUp() throws Exception {
        repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/runtime/concurrentExecution.bpmn20.xml").deploy();

        concurrentProcessInstanceIds = new ArrayList<>();
        sequentialProcessInstanceIds = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            concurrentProcessInstanceIds.add(runtimeService.startProcessInstanceByKey(CONCURRENT_PROCESS_KEY, "BUSINESS-KEY-" + i).getId());
        }
        sequentialProcessInstanceIds.add(runtimeService.startProcessInstanceByKey(SEQUENTIAL_PROCESS_KEY).getId());
    }

    @AfterEach
    protected void tearDown() throws Exception {
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    public void testQueryByProcessDefinitionKey() {
        // Concurrent process with 3 executions for each process instance
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).list()).hasSize(12);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(SEQUENTIAL_PROCESS_KEY).list()).hasSize(2);
    }

    @Test
    public void testQueryByProcessDefinitionKeyIn() {
        Set<String> includeIds = new HashSet<>();
        assertThat(runtimeService.createExecutionQuery().processDefinitionKeys(includeIds).list()).hasSize(14);
        includeIds.add(CONCURRENT_PROCESS_KEY);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKeys(includeIds).list()).hasSize(12);
        includeIds.add(SEQUENTIAL_PROCESS_KEY);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKeys(includeIds).list()).hasSize(14);
        includeIds.add("invalid");
        assertThat(runtimeService.createExecutionQuery().processDefinitionKeys(includeIds).list()).hasSize(14);

        includeIds.clear();
        includeIds.add("invalid");
        assertThat(runtimeService.createExecutionQuery().processDefinitionKeys(includeIds).list()).isEmpty();
    }
    
    @Test
    public void testQueryByExcludeProcessDefinitionKeys() {
        Set<String> excludeKeys = new HashSet<>();
        excludeKeys.add(CONCURRENT_PROCESS_KEY);
        excludeKeys.add(SEQUENTIAL_PROCESS_KEY);
        assertThat(runtimeService.createExecutionQuery().excludeProcessDefinitionKeys(excludeKeys).list()).hasSize(0);
        
        excludeKeys = new HashSet<>();
        excludeKeys.add(CONCURRENT_PROCESS_KEY);
        assertThat(runtimeService.createExecutionQuery().excludeProcessDefinitionKeys(excludeKeys).list()).hasSize(2);
        
        excludeKeys = new HashSet<>();
        excludeKeys.add(SEQUENTIAL_PROCESS_KEY);
        assertThat(runtimeService.createExecutionQuery().excludeProcessDefinitionKeys(excludeKeys).list()).hasSize(12);
        
        excludeKeys = new HashSet<>();
        excludeKeys.add("invalid");
        assertThat(runtimeService.createExecutionQuery().excludeProcessDefinitionKeys(excludeKeys).list()).hasSize(14);
    }

    @Test
    public void testQueryByInvalidProcessDefinitionKey() {
        ExecutionQuery query = runtimeService.createExecutionQuery().processDefinitionKey("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByProcessDefinitionCategory() {
        // Concurrent process with 3 executions for each process instance
        assertThat(runtimeService.createExecutionQuery().processDefinitionCategory(CONCURRENT_PROCESS_CATEGORY).list()).hasSize(12);
        assertThat(runtimeService.createExecutionQuery().processDefinitionCategory(SEQUENTIAL_PROCESS_CATEGORY).list()).hasSize(2);
    }

    @Test
    public void testQueryByInvalidProcessDefinitionCategory() {
        ExecutionQuery query = runtimeService.createExecutionQuery().processDefinitionCategory("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByProcessDefinitionName() {
        // Concurrent process with 3 executions for each process instance
        assertThat(runtimeService.createExecutionQuery().processDefinitionName(CONCURRENT_PROCESS_NAME).list()).hasSize(12);
        assertThat(runtimeService.createExecutionQuery().processDefinitionName(SEQUENTIAL_PROCESS_NAME).list()).hasSize(2);
    }

    @Test
    public void testQueryByInvalidProcessDefinitionName() {
        ExecutionQuery query = runtimeService.createExecutionQuery().processDefinitionName("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByProcessInstanceId() {
        for (String processInstanceId : concurrentProcessInstanceIds) {
            ExecutionQuery query = runtimeService.createExecutionQuery().processInstanceId(processInstanceId);
            assertThat(query.list()).hasSize(3);
            assertThat(query.count()).isEqualTo(3);
        }
        assertThat(runtimeService.createExecutionQuery().processInstanceId(sequentialProcessInstanceIds.get(0)).list()).hasSize(2);
    }

    @Test
    public void testQueryByProcessInstanceIds() {
        assertThat(runtimeService.createExecutionQuery().processInstanceIds(Set.of(concurrentProcessInstanceIds.get(0), concurrentProcessInstanceIds.get(1), "someId")).list()).extracting(Execution::getProcessInstanceId, Execution::getActivityId).containsExactlyInAnyOrder(
                tuple(concurrentProcessInstanceIds.get(0), "shipOrder"),
                tuple(concurrentProcessInstanceIds.get(0), null),
                tuple(concurrentProcessInstanceIds.get(0), "receivePayment"),
                tuple(concurrentProcessInstanceIds.get(1), null),
                tuple(concurrentProcessInstanceIds.get(1), "receivePayment"),
                tuple(concurrentProcessInstanceIds.get(1), "shipOrder")
        );

        assertThat(runtimeService.createExecutionQuery().processInstanceId(sequentialProcessInstanceIds.get(0)).list()).hasSize(2);
    }

    @Test
    public void testQueryByRootProcessInstanceId() {
        for (String processInstanceId : concurrentProcessInstanceIds) {
            ExecutionQuery query = runtimeService.createExecutionQuery().rootProcessInstanceId(processInstanceId);
            assertThat(query.list()).hasSize(3);
            assertThat(query.count()).isEqualTo(3);
        }
        assertThat(runtimeService.createExecutionQuery().rootProcessInstanceId(sequentialProcessInstanceIds.get(0)).list()).hasSize(2);
    }

    @Test
    public void testQueryByParentId() {
        // Concurrent processes fork into 2 child-executions. Should be found
        // when parentId is used
        for (String processInstanceId : concurrentProcessInstanceIds) {
            ExecutionQuery query = runtimeService.createExecutionQuery().parentId(processInstanceId);
            assertThat(query.list()).hasSize(2);
            assertThat(query.count()).isEqualTo(2);
        }
    }

    @Test
    public void testQueryByInvalidProcessInstanceId() {
        ExecutionQuery query = runtimeService.createExecutionQuery().processInstanceId("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryExecutionId() {
        List<Execution> executions = runtimeService.createExecutionQuery().processDefinitionKey(SEQUENTIAL_PROCESS_KEY).list();
        assertThat(executions).hasSize(2);
    }

    @Test
    public void testQueryByInvalidExecutionId() {
        ExecutionQuery query = runtimeService.createExecutionQuery().executionId("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByActivityId() {
        ExecutionQuery query = runtimeService.createExecutionQuery().activityId("receivePayment");
        assertThat(query.list()).hasSize(4);
        assertThat(query.count()).isEqualTo(4);

        assertThatThrownBy(() -> query.singleResult())
                .isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByInvalidActivityId() {
        ExecutionQuery query = runtimeService.createExecutionQuery().activityId("invalid");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    /**
     * Validate fix for ACT-1896
     */
    @Test
    public void testQueryByActivityIdAndBusinessKeyWithChildren() {
        ExecutionQuery query = runtimeService.createExecutionQuery().activityId("receivePayment").processInstanceBusinessKey("BUSINESS-KEY-1", true);
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);

        Execution execution = query.singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getActivityId()).isEqualTo("receivePayment");
    }

    @Test
    public void testQueryPaging() {
        assertThat(runtimeService.createExecutionQuery().count()).isEqualTo(14);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).listPage(0, 4)).hasSize(4);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).listPage(2, 1)).hasSize(1);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).listPage(1, 10)).hasSize(10);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).listPage(0, 20)).hasSize(12);
    }

    @Test
    public void testQuerySorting() {

        // 13 executions: 3 for each concurrent, 1 for the sequential
        assertThat(runtimeService.createExecutionQuery().orderByProcessInstanceId().asc().list()).hasSize(14);
        assertThat(runtimeService.createExecutionQuery().orderByProcessDefinitionId().asc().list()).hasSize(14);
        assertThat(runtimeService.createExecutionQuery().orderByProcessDefinitionKey().asc().list()).hasSize(14);

        assertThat(runtimeService.createExecutionQuery().orderByProcessInstanceId().desc().list()).hasSize(14);
        assertThat(runtimeService.createExecutionQuery().orderByProcessDefinitionId().desc().list()).hasSize(14);
        assertThat(runtimeService.createExecutionQuery().orderByProcessDefinitionKey().desc().list()).hasSize(14);

        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).orderByProcessDefinitionId().asc().list()).hasSize(12);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).orderByProcessDefinitionId().desc().list()).hasSize(12);

        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).orderByProcessDefinitionKey().asc()
                .orderByProcessInstanceId().desc().list()).hasSize(12);
    }

    @Test
    public void testQueryInvalidSorting() {
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().orderByProcessDefinitionKey().list())
                .isInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByBusinessKey() {
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).processInstanceBusinessKey("BUSINESS-KEY-1").list())
                .hasSize(1);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).processInstanceBusinessKey("BUSINESS-KEY-2").list())
                .hasSize(1);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).processInstanceBusinessKey("NON-EXISTING").list())
                .isEmpty();
    }

    @Test
    public void testQueryByBusinessKeyIncludingChildExecutions() {
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).processInstanceBusinessKey("BUSINESS-KEY-1", true).list())
                .hasSize(3);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).processInstanceBusinessKey("BUSINESS-KEY-2", true).list())
                .hasSize(3);
        assertThat(runtimeService.createExecutionQuery().processDefinitionKey(CONCURRENT_PROCESS_KEY).processInstanceBusinessKey("NON-EXISTING", true).list())
                .isEmpty();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryStringVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("stringVar", "abcdef");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("stringVar", "abcdef");
        vars.put("stringVar2", "ghijkl");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("stringVar", "azerty");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Test EQUAL on single string variable, should result in 2 matches
        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("stringVar", "abcdef");
        List<Execution> executions = query.list();
        assertThat(executions).hasSize(2);

        // Test EQUAL on two string variables, should result in single match
        query = runtimeService.createExecutionQuery().variableValueEquals("stringVar", "abcdef").variableValueEquals("stringVar2", "ghijkl");
        Execution execution = query.singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance2.getId());

        // Test NOT_EQUAL, should return only 1 execution
        execution = runtimeService.createExecutionQuery().variableValueNotEquals("stringVar", "abcdef").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN, should return only matching 'azerty'
        execution = runtimeService.createExecutionQuery().variableValueGreaterThan("stringVar", "abcdef").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueGreaterThan("stringVar", "z").singleResult();
        assertThat(execution).isNull();

        // Test GREATER_THAN_OR_EQUAL, should return 3 results
        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("stringVar", "abcdef").count()).isEqualTo(3);
        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("stringVar", "z").count()).isZero();

        // Test LESS_THAN, should return 2 results
        executions = runtimeService.createExecutionQuery().variableValueLessThan("stringVar", "abcdeg").list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("stringVar", "abcdef").count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("stringVar", "z").count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("stringVar", "abcdef").list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("stringVar", "z").count()).isEqualTo(3);
        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("stringVar", "aa").count()).isZero();

        // Test LIKE
        execution = runtimeService.createExecutionQuery().variableValueLike("stringVar", "azert%").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueLike("stringVar", "%y").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueLike("stringVar", "%zer%").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueLike("stringVar", "a%").count()).isEqualTo(3);
        assertThat(runtimeService.createExecutionQuery().variableValueLike("stringVar", "%x%").count()).isZero();

        // Test value-only matching
        execution = runtimeService.createExecutionQuery().variableValueEquals("azerty").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        executions = runtimeService.createExecutionQuery().variableValueEquals("abcdef").list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        execution = runtimeService.createExecutionQuery().variableValueEquals("notmatchinganyvalues").singleResult();
        assertThat(execution).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryEqualsIgnoreCase() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mixed", "AbCdEfG");
        vars.put("lower", "ABCDEFG");
        vars.put("upper", "abcdefg");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Execution execution = runtimeService.createExecutionQuery().variableValueEqualsIgnoreCase("mixed", "abcdefg").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        execution = runtimeService.createExecutionQuery().variableValueEqualsIgnoreCase("lower", "abcdefg").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        execution = runtimeService.createExecutionQuery().variableValueEqualsIgnoreCase("upper", "abcdefg").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        // Pass in non-lower-case string
        execution = runtimeService.createExecutionQuery().variableValueEqualsIgnoreCase("upper", "ABCdefg").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        // Pass in null-value, should cause exception
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueEqualsIgnoreCase("upper", null).singleResult())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("value is null");

        // Pass in null name, should cause exception
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueEqualsIgnoreCase(null, "abcdefg").singleResult())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");

        // Test NOT equals
        execution = runtimeService.createExecutionQuery().variableValueNotEqualsIgnoreCase("upper", "UIOP").singleResult();
        assertThat(execution).isNotNull();

        // Should return result when using "ABCdefg" case-insensitive while
        // normal not-equals won't
        execution = runtimeService.createExecutionQuery().variableValueNotEqualsIgnoreCase("upper", "ABCdefg").singleResult();
        assertThat(execution).isNull();
        execution = runtimeService.createExecutionQuery().variableValueNotEquals("upper", "ABCdefg").singleResult();
        assertThat(execution).isNotNull();

        // Pass in null-value, should cause exception
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueNotEqualsIgnoreCase("upper", null).singleResult())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("value is null");

        // Pass in null name, should cause exception
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueNotEqualsIgnoreCase(null, "abcdefg").singleResult())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryLike() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("var1", "aaaaa");
        vars.put("var2", "bbbbb");
        vars.put("var3", "ccccc");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Execution execution = runtimeService.createExecutionQuery().variableValueLike("var1", "aa%").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        execution = runtimeService.createExecutionQuery().variableValueLike("var2", "bb%").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        // Pass in null-value, should cause exception
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueLike("var1", null).singleResult())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Only string values can be used with 'like' condition");

        // Pass in null name, should cause exception
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueLike(null, "abcdefg").singleResult())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryLikeIgnoreCase() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mixed", "AbCdEfG");
        vars.put("lower", "ABCDEFG");
        vars.put("upper", "abcdefg");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Execution execution = runtimeService.createExecutionQuery().variableValueLikeIgnoreCase("mixed", "abcde%").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        execution = runtimeService.createExecutionQuery().variableValueLikeIgnoreCase("lower", "abcd%").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        execution = runtimeService.createExecutionQuery().variableValueLikeIgnoreCase("upper", "abcd%").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        // Pass in non-lower-case string
        execution = runtimeService.createExecutionQuery().variableValueLikeIgnoreCase("upper", "ABCde%").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        // Pass in null-value, should cause exception
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueEqualsIgnoreCase("upper", null).singleResult())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("value is null");

        // Pass in null name, should cause exception
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueEqualsIgnoreCase(null, "abcdefg").singleResult())
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryLongVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("longVar", 12345L);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("longVar", 12345L);
        vars.put("longVar2", 67890L);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("longVar", 55555L);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on single long variable, should result in 2 matches
        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("longVar", 12345L);
        List<Execution> executions = query.list();
        assertThat(executions).hasSize(2);

        // Query on two long variables, should result in single match
        query = runtimeService.createExecutionQuery().variableValueEquals("longVar", 12345L).variableValueEquals("longVar2", 67890L);
        Execution execution = query.singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        execution = runtimeService.createExecutionQuery().variableValueEquals("longVar", 999L).singleResult();
        assertThat(execution).isNull();

        // Test NOT_EQUALS
        execution = runtimeService.createExecutionQuery().variableValueNotEquals("longVar", 12345L).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        execution = runtimeService.createExecutionQuery().variableValueGreaterThan("longVar", 44444L).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("longVar", 55555L).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("longVar", 1L).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("longVar", 44444L).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("longVar", 55555L).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("longVar", 1L).count()).isEqualTo(3);

        // Test LESS_THAN
        executions = runtimeService.createExecutionQuery().variableValueLessThan("longVar", 55555L).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("longVar", 12345L).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("longVar", 66666L).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("longVar", 55555L).list();
        assertThat(executions).hasSize(3);

        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("longVar", 12344L).count()).isZero();

        // Test value-only matching
        execution = runtimeService.createExecutionQuery().variableValueEquals(55555L).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        executions = runtimeService.createExecutionQuery().variableValueEquals(12345L).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        execution = runtimeService.createExecutionQuery().variableValueEquals(99999L).singleResult();
        assertThat(execution).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryDoubleVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("doubleVar", 12345.6789);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("doubleVar", 12345.6789);
        vars.put("doubleVar2", 9876.54321);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("doubleVar", 55555.5555);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on single double variable, should result in 2 matches
        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("doubleVar", 12345.6789);
        List<Execution> executions = query.list();
        assertThat(executions).hasSize(2);

        // Query on two double variables, should result in single value
        query = runtimeService.createExecutionQuery().variableValueEquals("doubleVar", 12345.6789).variableValueEquals("doubleVar2", 9876.54321);
        Execution execution = query.singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        execution = runtimeService.createExecutionQuery().variableValueEquals("doubleVar", 9999.99).singleResult();
        assertThat(execution).isNull();

        // Test NOT_EQUALS
        execution = runtimeService.createExecutionQuery().variableValueNotEquals("doubleVar", 12345.6789).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        execution = runtimeService.createExecutionQuery().variableValueGreaterThan("doubleVar", 44444.4444).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("doubleVar", 55555.5555).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("doubleVar", 1.234).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("doubleVar", 44444.4444).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("doubleVar", 55555.5555).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("doubleVar", 1.234).count()).isEqualTo(3);

        // Test LESS_THAN
        executions = runtimeService.createExecutionQuery().variableValueLessThan("doubleVar", 55555.5555).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("doubleVar", 12345.6789).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("doubleVar", 66666.6666).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("doubleVar", 55555.5555).list();
        assertThat(executions).hasSize(3);

        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("doubleVar", 12344.6789).count()).isZero();

        // Test value-only matching
        execution = runtimeService.createExecutionQuery().variableValueEquals(55555.5555).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        executions = runtimeService.createExecutionQuery().variableValueEquals(12345.6789).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        execution = runtimeService.createExecutionQuery().variableValueEquals(9999.9999).singleResult();
        assertThat(execution).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryIntegerVariable() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("integerVar", 12345);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("integerVar", 12345);
        vars.put("integerVar2", 67890);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("integerVar", 55555);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on single integer variable, should result in 2 matches
        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("integerVar", 12345);
        List<Execution> executions = query.list();
        assertThat(executions).hasSize(2);

        // Query on two integer variables, should result in single value
        query = runtimeService.createExecutionQuery().variableValueEquals("integerVar", 12345).variableValueEquals("integerVar2", 67890);
        Execution execution = query.singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        execution = runtimeService.createExecutionQuery().variableValueEquals("integerVar", 9999).singleResult();
        assertThat(execution).isNull();

        // Test NOT_EQUALS
        execution = runtimeService.createExecutionQuery().variableValueNotEquals("integerVar", 12345).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        execution = runtimeService.createExecutionQuery().variableValueGreaterThan("integerVar", 44444).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("integerVar", 55555).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("integerVar", 1).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("integerVar", 44444).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("integerVar", 55555).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("integerVar", 1).count()).isEqualTo(3);

        // Test LESS_THAN
        executions = runtimeService.createExecutionQuery().variableValueLessThan("integerVar", 55555).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("integerVar", 12345).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("integerVar", 66666).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("integerVar", 55555).list();
        assertThat(executions).hasSize(3);

        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("integerVar", 12344).count()).isZero();

        // Test value-only matching
        execution = runtimeService.createExecutionQuery().variableValueEquals(55555).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        executions = runtimeService.createExecutionQuery().variableValueEquals(12345).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        execution = runtimeService.createExecutionQuery().variableValueEquals(99999).singleResult();
        assertThat(execution).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryShortVariable() {
        Map<String, Object> vars = new HashMap<>();
        short shortVar = 1234;
        vars.put("shortVar", shortVar);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        short shortVar2 = 6789;
        vars = new HashMap<>();
        vars.put("shortVar", shortVar);
        vars.put("shortVar2", shortVar2);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("shortVar", (short) 5555);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on single short variable, should result in 2 matches
        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("shortVar", shortVar);
        List<Execution> executions = query.list();
        assertThat(executions).hasSize(2);

        // Query on two short variables, should result in single value
        query = runtimeService.createExecutionQuery().variableValueEquals("shortVar", shortVar).variableValueEquals("shortVar2", shortVar2);
        Execution execution = query.singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        short unexistingValue = (short) 9999;
        execution = runtimeService.createExecutionQuery().variableValueEquals("shortVar", unexistingValue).singleResult();
        assertThat(execution).isNull();

        // Test NOT_EQUALS
        execution = runtimeService.createExecutionQuery().variableValueNotEquals("shortVar", (short) 1234).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        execution = runtimeService.createExecutionQuery().variableValueGreaterThan("shortVar", (short) 4444).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("shortVar", (short) 5555).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("shortVar", (short) 1).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("shortVar", (short) 4444).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("shortVar", (short) 5555).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("shortVar", (short) 1).count()).isEqualTo(3);

        // Test LESS_THAN
        executions = runtimeService.createExecutionQuery().variableValueLessThan("shortVar", (short) 5555).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());
        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("shortVar", (short) 1234).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("shortVar", (short) 6666).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("shortVar", (short) 5555).list();
        assertThat(executions).hasSize(3);

        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("shortVar", (short) 1233).count()).isZero();

        // Test value-only matching
        execution = runtimeService.createExecutionQuery().variableValueEquals((short) 5555).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        executions = runtimeService.createExecutionQuery().variableValueEquals((short) 1234).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        execution = runtimeService.createExecutionQuery().variableValueEquals((short) 999).singleResult();
        assertThat(execution).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryDateVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        Date date1 = Calendar.getInstance().getTime();
        vars.put("dateVar", date1);

        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Calendar cal2 = Calendar.getInstance();
        cal2.add(Calendar.SECOND, 1);

        Date date2 = cal2.getTime();
        vars = new HashMap<>();
        vars.put("dateVar", date1);
        vars.put("dateVar2", date2);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.YEAR, 1);
        vars = new HashMap<>();
        vars.put("dateVar", nextYear.getTime());
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Calendar nextMonth = Calendar.getInstance();
        nextMonth.add(Calendar.MONTH, 1);

        Calendar twoYearsLater = Calendar.getInstance();
        twoYearsLater.add(Calendar.YEAR, 2);

        Calendar oneYearAgo = Calendar.getInstance();
        oneYearAgo.add(Calendar.YEAR, -1);

        // Query on single short variable, should result in 2 matches
        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("dateVar", date1);
        List<Execution> executions = query.list();
        assertThat(executions).hasSize(2);

        // Query on two short variables, should result in single value
        query = runtimeService.createExecutionQuery().variableValueEquals("dateVar", date1).variableValueEquals("dateVar2", date2);
        Execution execution = query.singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        Date unexistingDate = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss").parse("01/01/1989 12:00:00");
        execution = runtimeService.createExecutionQuery().variableValueEquals("dateVar", unexistingDate).singleResult();
        assertThat(execution).isNull();

        // Test NOT_EQUALS
        execution = runtimeService.createExecutionQuery().variableValueNotEquals("dateVar", date1).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        execution = runtimeService.createExecutionQuery().variableValueGreaterThan("dateVar", nextMonth.getTime()).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("dateVar", nextYear.getTime()).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("dateVar", nextMonth.getTime()).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("dateVar", nextYear.getTime()).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isEqualTo(3);

        // Test LESS_THAN
        executions = runtimeService.createExecutionQuery().variableValueLessThan("dateVar", nextYear.getTime()).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("dateVar", date1).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("dateVar", twoYearsLater.getTime()).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("dateVar", nextYear.getTime()).list();
        assertThat(executions).hasSize(3);

        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("dateVar", oneYearAgo.getTime()).count()).isZero();

        // Test value-only matching
        execution = runtimeService.createExecutionQuery().variableValueEquals(nextYear.getTime()).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        executions = runtimeService.createExecutionQuery().variableValueEquals(date1).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactlyInAnyOrder(processInstance1.getId(), processInstance2.getId());

        execution = runtimeService.createExecutionQuery().variableValueEquals(twoYearsLater.getTime()).singleResult();
        assertThat(execution).isNull();

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryInstantVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        Instant instant1 = Instant.now();
        vars.put("instantVar", instant1);

        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Instant instant2 = instant1.plusSeconds(1);
        vars = new HashMap<>();
        vars.put("instantVar", instant1);
        vars.put("instantVar2", instant2);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Instant nextYear = instant1.plus(365, ChronoUnit.DAYS);
        vars = new HashMap<>();
        vars.put("instantVar", nextYear);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Instant nextMonth = instant1.plus(30, ChronoUnit.DAYS);

        Instant twoYearsLater = instant1.plus(730, ChronoUnit.DAYS);

        Instant oneYearAgo = instant1.minus(365, ChronoUnit.DAYS);

        // Query on single instant variable, should result in 2 matches
        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("instantVar", instant1);
        List<Execution> executions = query.list();
        assertThat(executions).hasSize(2);

        // Query on two instant variables, should result in single value
        query = runtimeService.createExecutionQuery().variableValueEquals("instantVar", instant1).variableValueEquals("instantVar2", instant2);
        Execution execution = query.singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        execution = runtimeService.createExecutionQuery().variableValueEquals("instantVar", instant1.minus(1, ChronoUnit.HOURS)).singleResult();
        assertThat(execution).isNull();

        // Test NOT_EQUALS
        execution = runtimeService.createExecutionQuery().variableValueNotEquals("instantVar", instant1).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        execution = runtimeService.createExecutionQuery().variableValueGreaterThan("instantVar", nextMonth).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("instantVar", nextYear).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("instantVar", oneYearAgo).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("instantVar", nextMonth).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("instantVar", nextYear).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("instantVar", oneYearAgo).count()).isEqualTo(3);

        // Test LESS_THAN
        executions = runtimeService.createExecutionQuery().variableValueLessThan("instantVar", nextYear).list();
        assertThat(executions)
            .extracting(Execution::getId)
            .containsExactlyInAnyOrder(
                processInstance1.getId(),
                processInstance2.getId()
            );

        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("instantVar", instant1).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("instantVar", twoYearsLater).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("instantVar", nextYear).list();
        assertThat(executions).hasSize(3);

        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("instantVar", oneYearAgo).count()).isZero();

        // Test value-only matching
        execution = runtimeService.createExecutionQuery().variableValueEquals(nextYear).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        executions = runtimeService.createExecutionQuery().variableValueEquals(instant1).list();
        assertThat(executions)
            .extracting(Execution::getId)
            .containsExactlyInAnyOrder(
                processInstance1.getId(),
                processInstance2.getId()
            );

        execution = runtimeService.createExecutionQuery().variableValueEquals(twoYearsLater).singleResult();
        assertThat(execution).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryLocalDateVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        LocalDate localDate = LocalDate.now();
        vars.put("localDateVar", localDate);

        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        LocalDate localDate2 = localDate.plusDays(1);
        vars = new HashMap<>();
        vars.put("localDateVar", localDate);
        vars.put("localDateVar2", localDate2);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        LocalDate nextYear = localDate.plusYears(1);
        vars = new HashMap<>();
        vars.put("localDateVar", nextYear);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        LocalDate nextMonth = localDate.plusMonths(1);

        LocalDate twoYearsLater = localDate.plusYears(2);

        LocalDate oneYearAgo = localDate.minusYears(1);

        // Query on single localDate variable, should result in 2 matches
        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("localDateVar", localDate);
        List<Execution> executions = query.list();
        assertThat(executions).hasSize(2);

        // Query on two localDate variables, should result in single value
        query = runtimeService.createExecutionQuery().variableValueEquals("localDateVar", localDate).variableValueEquals("localDateVar2", localDate2);
        Execution execution = query.singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        execution = runtimeService.createExecutionQuery().variableValueEquals("localDateVar", localDate.minusDays(1)).singleResult();
        assertThat(execution).isNull();

        // Test NOT_EQUALS
        execution = runtimeService.createExecutionQuery().variableValueNotEquals("localDateVar", localDate).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        execution = runtimeService.createExecutionQuery().variableValueGreaterThan("localDateVar", nextMonth).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("localDateVar", nextYear).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("localDateVar", oneYearAgo).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("localDateVar", nextMonth).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("localDateVar", nextYear).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("localDateVar", oneYearAgo).count()).isEqualTo(3);

        // Test LESS_THAN
        executions = runtimeService.createExecutionQuery().variableValueLessThan("localDateVar", nextYear).list();
        assertThat(executions)
            .extracting(Execution::getId)
            .containsExactlyInAnyOrder(
                processInstance1.getId(),
                processInstance2.getId()
            );

        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("localDateVar", localDate).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("localDateVar", twoYearsLater).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("localDateVar", nextYear).list();
        assertThat(executions).hasSize(3);

        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("localDateVar", oneYearAgo).count()).isZero();

        // Test value-only matching
        execution = runtimeService.createExecutionQuery().variableValueEquals(nextYear).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        executions = runtimeService.createExecutionQuery().variableValueEquals(localDate).list();
        assertThat(executions)
            .extracting(Execution::getId)
            .containsExactlyInAnyOrder(
                processInstance1.getId(),
                processInstance2.getId()
            );

        execution = runtimeService.createExecutionQuery().variableValueEquals(twoYearsLater).singleResult();
        assertThat(execution).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryLocalDateTimeVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        LocalDateTime localDateTime = LocalDateTime.now();
        vars.put("localDateTimeVar", localDateTime);

        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        LocalDateTime localDateTime2 = localDateTime.plusDays(1);
        vars = new HashMap<>();
        vars.put("localDateTimeVar", localDateTime);
        vars.put("localDateTimeVar2", localDateTime2);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        LocalDateTime nextYear = localDateTime.plusYears(1);
        vars = new HashMap<>();
        vars.put("localDateTimeVar", nextYear);
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        LocalDateTime nextMonth = localDateTime.plusMonths(1);

        LocalDateTime twoYearsLater = localDateTime.plusYears(2);

        LocalDateTime oneYearAgo = localDateTime.minusYears(1);

        // Query on single localDateTime variable, should result in 2 matches
        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("localDateTimeVar", localDateTime);
        List<Execution> executions = query.list();
        assertThat(executions).hasSize(2);

        // Query on two localDateTime variables, should result in single value
        query = runtimeService.createExecutionQuery().variableValueEquals("localDateTimeVar", localDateTime).variableValueEquals("localDateTimeVar2", localDateTime2);
        Execution execution = query.singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance2.getId());

        // Query with unexisting variable value
        execution = runtimeService.createExecutionQuery().variableValueEquals("localDateTimeVar", localDateTime.minusDays(1)).singleResult();
        assertThat(execution).isNull();

        // Test NOT_EQUALS
        execution = runtimeService.createExecutionQuery().variableValueNotEquals("localDateTimeVar", localDateTime).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        // Test GREATER_THAN
        execution = runtimeService.createExecutionQuery().variableValueGreaterThan("localDateTimeVar", nextMonth).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("localDateTimeVar", nextYear).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThan("localDateTimeVar", oneYearAgo).count()).isEqualTo(3);

        // Test GREATER_THAN_OR_EQUAL
        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("localDateTimeVar", nextMonth).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        execution = runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("localDateTimeVar", nextYear).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        assertThat(runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("localDateTimeVar", oneYearAgo).count()).isEqualTo(3);

        // Test LESS_THAN
        executions = runtimeService.createExecutionQuery().variableValueLessThan("localDateTimeVar", nextYear).list();
        assertThat(executions)
            .extracting(Execution::getId)
            .containsExactlyInAnyOrder(
                processInstance1.getId(),
                processInstance2.getId()
            );

        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("localDateTimeVar", localDateTime).count()).isZero();
        assertThat(runtimeService.createExecutionQuery().variableValueLessThan("localDateTimeVar", twoYearsLater).count()).isEqualTo(3);

        // Test LESS_THAN_OR_EQUAL
        executions = runtimeService.createExecutionQuery().variableValueLessThanOrEqual("localDateTimeVar", nextYear).list();
        assertThat(executions).hasSize(3);

        assertThat(runtimeService.createExecutionQuery().variableValueLessThanOrEqual("localDateTimeVar", oneYearAgo).count()).isZero();

        // Test value-only matching
        execution = runtimeService.createExecutionQuery().variableValueEquals(nextYear).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance3.getId());

        executions = runtimeService.createExecutionQuery().variableValueEquals(localDateTime).list();
        assertThat(executions)
            .extracting(Execution::getId)
            .containsExactlyInAnyOrder(
                processInstance1.getId(),
                processInstance2.getId()
            );

        execution = runtimeService.createExecutionQuery().variableValueEquals(twoYearsLater).singleResult();
        assertThat(execution).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testBooleanVariable() throws Exception {

        // TEST EQUALS
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("booleanVar", true);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("booleanVar", false);
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().variableValueEquals("booleanVar", true).list();
        assertThat(instances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance1.getId());

        instances = runtimeService.createProcessInstanceQuery().variableValueEquals("booleanVar", false).list();
        assertThat(instances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance2.getId());

        // TEST NOT_EQUALS
        instances = runtimeService.createProcessInstanceQuery().variableValueNotEquals("booleanVar", true).list();
        assertThat(instances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance2.getId());

        instances = runtimeService.createProcessInstanceQuery().variableValueNotEquals("booleanVar", false).list();
        assertThat(instances)
                .extracting(ProcessInstance::getId)
                .containsExactly(processInstance1.getId());

        // Test value-only matching
        Execution execution = runtimeService.createExecutionQuery().variableValueEquals(true).singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getId()).isEqualTo(processInstance1.getId());

        // Test unsupported operations
        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueGreaterThan("booleanVar", true))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'greater than' condition");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueGreaterThanOrEqual("booleanVar", true))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'greater than or equal' condition");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLessThan("booleanVar", true))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'less than' condition");

        assertThatThrownBy(() -> runtimeService.createProcessInstanceQuery().variableValueLessThanOrEqual("booleanVar", true))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'less than or equal' condition");

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryVariablesUpdatedToNullValue() {
        // Start process instance with different types of variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("longVar", 928374L);
        variables.put("shortVar", (short) 123);
        variables.put("integerVar", 1234);
        variables.put("stringVar", "coca-cola");
        variables.put("booleanVar", true);
        variables.put("dateVar", new Date());
        variables.put("nullVar", null);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", variables);

        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("longVar", null).variableValueEquals("shortVar", null)
                .variableValueEquals("integerVar", null)
                .variableValueEquals("stringVar", null).variableValueEquals("booleanVar", null).variableValueEquals("dateVar", null);

        ExecutionQuery notQuery = runtimeService.createExecutionQuery().variableValueNotEquals("longVar", null).variableValueNotEquals("shortVar", null)
                .variableValueNotEquals("integerVar", null)
                .variableValueNotEquals("stringVar", null).variableValueNotEquals("booleanVar", null).variableValueNotEquals("dateVar", null);

        assertThat(query.singleResult()).isNull();
        assertThat(notQuery.singleResult()).isNotNull();

        // Set all existing variables values to null
        runtimeService.setVariable(processInstance.getId(), "longVar", null);
        runtimeService.setVariable(processInstance.getId(), "shortVar", null);
        runtimeService.setVariable(processInstance.getId(), "integerVar", null);
        runtimeService.setVariable(processInstance.getId(), "stringVar", null);
        runtimeService.setVariable(processInstance.getId(), "booleanVar", null);
        runtimeService.setVariable(processInstance.getId(), "dateVar", null);
        runtimeService.setVariable(processInstance.getId(), "nullVar", null);

        Execution queryResult = query.singleResult();
        assertThat(queryResult).isNotNull();
        assertThat(queryResult.getId()).isEqualTo(processInstance.getId());
        assertThat(notQuery.singleResult()).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryNullVariable() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("nullVar", null);
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("nullVar", "notnull");
        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("nullVarLong", "notnull");
        ProcessInstance processInstance3 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("nullVarDouble", "notnull");
        ProcessInstance processInstance4 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        vars = new HashMap<>();
        vars.put("nullVarByte", "testbytes".getBytes());
        ProcessInstance processInstance5 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        // Query on null value, should return one value
        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("nullVar", null);
        List<Execution> executions = query.list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactly(processInstance1.getId());

        // Test NOT_EQUALS null
        assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("nullVar", null).count()).isEqualTo(1);
        assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("nullVarLong", null).count()).isEqualTo(1);
        assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("nullVarDouble", null).count()).isEqualTo(1);
        // When a byte-array reference is present, the variable is not considered null
        assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("nullVarByte", null).count()).isEqualTo(1);

        // Test value-only matching
        Execution execution = runtimeService.createExecutionQuery().variableValueEquals(null).singleResult();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactly(processInstance1.getId());

        // All other variable queries with null should throw exception
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueGreaterThan("nullVar", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'greater than' condition");

        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual("nullVar", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'greater than or equal' condition");

        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueLessThan("nullVar", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'less than' condition");

        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueLessThanOrEqual("nullVar", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Booleans and null cannot be used in 'less than or equal' condition");

        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueLike("nullVar", null))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Only string values can be used with 'like' condition");

        runtimeService.deleteProcessInstance(processInstance1.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance3.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance4.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance5.getId(), "test");

        // Test value-only matching, non-null processes exist
        execution = runtimeService.createExecutionQuery().variableValueEquals(null).singleResult();
        assertThat(execution).isNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryInvalidTypes() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("bytesVar", "test".getBytes());
        vars.put("serializableVar", new DummySerializable());

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueEquals("bytesVar", "test".getBytes()).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Variables of type ByteArray cannot be used to query");

        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueEquals("serializableVar", new DummySerializable()).list())
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Variables of type ByteArray cannot be used to query");

        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    }

    @Test
    public void testQueryVariablesNullNameArgument() {
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueEquals(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueNotEquals(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueGreaterThan(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueGreaterThanOrEqual(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueLessThan(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueLessThanOrEqual(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
        assertThatThrownBy(() -> runtimeService.createExecutionQuery().variableValueLike(null, "value"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("name is null");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryAllVariableTypes() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("nullVar", null);
        vars.put("stringVar", "string");
        vars.put("longVar", 10L);
        vars.put("doubleVar", 1.2);
        vars.put("integerVar", 1234);
        vars.put("booleanVar", true);
        vars.put("shortVar", (short) 123);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        ExecutionQuery query = runtimeService.createExecutionQuery().variableValueEquals("nullVar", null).variableValueEquals("stringVar", "string")
                .variableValueEquals("longVar", 10L)
                .variableValueEquals("doubleVar", 1.2).variableValueEquals("integerVar", 1234).variableValueEquals("booleanVar", true)
                .variableValueEquals("shortVar", (short) 123);

        List<Execution> executions = query.list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactly(processInstance.getId());

        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testClashingValues() throws Exception {
        Map<String, Object> vars = new HashMap<>();
        vars.put("var", 1234L);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Map<String, Object> vars2 = new HashMap<>();
        vars2.put("var", 1234);

        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars2);

        List<Execution> executions = runtimeService.createExecutionQuery().processDefinitionKey("oneTaskProcess").variableValueEquals("var", 1234L).list();
        assertThat(executions)
                .extracting(Execution::getId)
                .containsExactly(processInstance.getId());

        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
        runtimeService.deleteProcessInstance(processInstance2.getId(), "test");
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testVariableExistsQuery() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("mixed", "AbCdEfG");
        vars.put("upper", "ABCDEFG");
        vars.put("lower", "abcdefg");
        ProcessInstance processInstance1 = runtimeService.startProcessInstanceByKey("oneTaskProcess", vars);

        Execution execution = runtimeService.createExecutionQuery().variableExists("mixed").singleResult();
        assertThat(execution).isNotNull();
        assertThat(execution.getProcessInstanceId()).isEqualTo(processInstance1.getId());

        runtimeService.setVariableLocal(execution.getId(), "localvar", "test");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).processVariableNotExists("lower").list();
        assertThat(executions).isEmpty();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).processVariableExists("lower")
                .processVariableValueEquals("upper", "ABCDEFG").list();
        assertThat(executions).hasSize(2);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).or().processVariableExists("mixed")
                .processVariableValueEquals("upper", "ABCDEFG").endOr().list();
        assertThat(executions).hasSize(2);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).or().processVariableNotExists("mixed")
                .processVariableValueEquals("upper", "ABCDEFG").endOr().list();
        assertThat(executions).hasSize(2);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).or().processVariableNotExists("mixed").endOr().or()
                .processVariableValueEquals("upper", "ABCDEFG").endOr().list();
        assertThat(executions).isEmpty();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).variableExists("localvar")
                .processVariableValueEquals("upper", "ABCDEFG").list();
        assertThat(executions).hasSize(1);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).or().variableExists("mixed")
                .processVariableValueEquals("upper", "ABCDEFG").endOr().list();
        assertThat(executions).hasSize(2);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).or().variableNotExists("mixed")
                .processVariableValueEquals("upper", "ABCDEFG").endOr().list();
        assertThat(executions).hasSize(2);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).or().variableNotExists("mixed").endOr().or()
                .variableValueEquals("upper", "ABCDEFG").endOr().list();
        assertThat(executions).isEmpty();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance1.getId()).or().variableExists("mixed").endOr().or()
                .variableValueEquals("upper", "ABCDEFG").endOr().list();
        assertThat(executions).hasSize(1);
    }

    @Test
    @Deployment
    public void testQueryBySignalSubscriptionName() {
        runtimeService.startProcessInstanceByKey("catchSignal");

        // it finds subscribed instances
        Execution execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").singleResult();
        assertThat(execution).isNotNull();

        // test query for nonexisting subscription
        execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("nonExisitng").singleResult();
        assertThat(execution).isNull();

        // it finds more than one
        runtimeService.startProcessInstanceByKey("catchSignal");
        assertThat(runtimeService.createExecutionQuery().signalEventSubscriptionName("alert").count()).isEqualTo(2);
    }

    @Test
    @Deployment
    public void testQueryBySignalSubscriptionNameBoundary() {
        runtimeService.startProcessInstanceByKey("signalProces");

        // it finds subscribed instances
        Execution execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("Test signal").singleResult();
        assertThat(execution).isNotNull();

        // test query for nonexisting subscription
        execution = runtimeService.createExecutionQuery().signalEventSubscriptionName("nonExisitng").singleResult();
        assertThat(execution).isNull();

        // it finds more than one
        runtimeService.startProcessInstanceByKey("signalProces");
        assertThat(runtimeService.createExecutionQuery().signalEventSubscriptionName("Test signal").count()).isEqualTo(2);
    }

    @Test
    public void testNativeQuery() {
        // just test that the query will be constructed and executed, details
        // are tested in the TaskQueryTest
        assertThat(managementService.getTableName(Execution.class, false)).isEqualTo("ACT_RU_EXECUTION");

        long executionCount = runtimeService.createExecutionQuery().count();

        assertThat(runtimeService.createNativeExecutionQuery().sql("SELECT * FROM " + managementService.getTableName(Execution.class)).list())
                .hasSize((int) executionCount);
        assertThat(runtimeService.createNativeExecutionQuery().sql("SELECT count(*) FROM " + managementService.getTableName(Execution.class)).count())
                .isEqualTo(executionCount);
    }

    @Test
    public void testNativeQueryPaging() {
        assertThat(runtimeService.createNativeExecutionQuery().sql("SELECT * FROM " + managementService.getTableName(Execution.class)).listPage(1, 5))
                .hasSize(5);
        assertThat(runtimeService.createNativeExecutionQuery().sql("SELECT * FROM " + managementService.getTableName(Execution.class)).listPage(2, 1))
                .hasSize(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/concurrentExecution.bpmn20.xml" })
    public void testExecutionQueryWithProcessVariable() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("x", "parent");
        variables.put("xIgnoreCase", "PaReNt");
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("concurrent", variables);

        List<Execution> concurrentExecutions = runtimeService.createExecutionQuery().processInstanceId(pi.getId()).list();
        assertThat(concurrentExecutions).hasSize(3);
        for (Execution execution : concurrentExecutions) {
            if (!((ExecutionEntity) execution).isProcessInstanceType()) {
                // only the concurrent executions, not the root one, would be
                // cooler to query that directly, see
                // https://activiti.atlassian.net/browse/ACT-1373
                runtimeService.setVariableLocal(execution.getId(), "x", "child");
                runtimeService.setVariableLocal(execution.getId(), "xIgnoreCase", "ChILD");
            }
        }

        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).variableValueEquals("x", "child").count()).isEqualTo(2);
        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).variableValueEquals("x", "parent").count()).isEqualTo(1);

        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).processVariableValueEquals("x", "parent").count()).isEqualTo(3);
        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).processVariableValueNotEquals("x", "xxx").count()).isEqualTo(3);

        // Test value-only query
        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).processVariableValueEquals("child").count()).isZero();
        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).processVariableValueEquals("parent").count()).isEqualTo(3);

        // Test ignore-case queries
        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).processVariableValueEqualsIgnoreCase("xIgnoreCase", "CHILD").count())
                .isZero();
        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).processVariableValueEqualsIgnoreCase("xIgnoreCase", "PARENT").count())
                .isEqualTo(3);

        // Test ignore-case queries
        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).processVariableValueNotEqualsIgnoreCase("xIgnoreCase", "paRent").count())
                .isZero();
        assertThat(runtimeService.createExecutionQuery().processInstanceId(pi.getId()).processVariableValueNotEqualsIgnoreCase("xIgnoreCase", "chilD").count())
                .isEqualTo(3);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/executionLocalization.bpmn20.xml" })
    public void testLocalizeExecution() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("executionLocalization");

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertThat(executions).hasSize(3);
        for (Execution execution : executions) {
            if (execution.getParentId() == null) {
                assertThat(execution.getName()).isNull();
                assertThat(execution.getDescription()).isNull();

            } else if (execution.getParentId().equals(execution.getProcessInstanceId())) {
                assertThat(execution.getName()).isNull();
                assertThat(execution.getDescription()).isNull();
            }
        }

        ObjectNode infoNode = dynamicBpmnService.getProcessDefinitionInfo(processInstance.getProcessDefinitionId());
        dynamicBpmnService.changeLocalizationName("en-GB", "executionLocalization", "Process Name 'en-GB'", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-GB", "executionLocalization", "Process Description 'en-GB'", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        dynamicBpmnService.changeLocalizationName("en", "executionLocalization", "Process Name 'en'", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en", "executionLocalization", "Process Description 'en'", infoNode);

        dynamicBpmnService.changeLocalizationName("en-GB", "subProcess", "SubProcess Name 'en-GB'", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-GB", "subProcess", "SubProcess Description 'en-GB'", infoNode);

        dynamicBpmnService.changeLocalizationName("en", "subProcess", "SubProcess Name 'en'", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en", "subProcess", "SubProcess Description 'en'", infoNode);

        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        String subProcessId = null;
        assertThat(executions).hasSize(3);
        for (Execution execution : executions) {
            if (execution.getParentId() == null) {
                assertThat(execution.getName()).isNull();
                assertThat(execution.getDescription()).isNull();

            } else if (execution.getParentId().equals(execution.getProcessInstanceId())) {
                assertThat(execution.getName()).isNull();
                assertThat(execution.getDescription()).isNull();
                subProcessId = execution.getId();
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).locale("es").list();
        assertThat(executions).hasSize(3);
        for (Execution execution : executions) {
            if (execution.getParentId() == null) {
                assertThat(execution.getName()).isEqualTo("Nombre del proceso");
                assertThat(execution.getDescription()).isEqualTo("Descripción del proceso");

            } else if (execution.getParentId().equals(execution.getProcessInstanceId())) {
                assertThat(execution.getName()).isEqualTo("Nombre Subproceso");
                assertThat(execution.getDescription()).isEqualTo("Subproceso Descripción");
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).locale("it").list();
        assertThat(executions).hasSize(3);
        for (Execution execution : executions) {
            if (execution.getParentId() == null) {
                assertThat(execution.getName()).isEqualTo("Nome del processo");
                assertThat(execution.getDescription()).isEqualTo("Descrizione del processo");

            } else if (execution.getParentId().equals(execution.getProcessInstanceId())) {
                assertThat(execution.getName()).isEqualTo("Nome sottoprocesso");
                assertThat(execution.getDescription()).isEqualTo("Sottoprocesso Descrizione");
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).locale("en-GB").list();
        assertThat(executions).hasSize(3);
        for (Execution execution : executions) {
            if (execution.getParentId() == null) {
                assertThat(execution.getName()).isEqualTo("Process Name 'en-GB'");
                assertThat(execution.getDescription()).isEqualTo("Process Description 'en-GB'");

            } else if (execution.getParentId().equals(execution.getProcessInstanceId())) {
                assertThat(execution.getName()).isEqualTo("SubProcess Name 'en-GB'");
                assertThat(execution.getDescription()).isEqualTo("SubProcess Description 'en-GB'");
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).listPage(0, 10);
        assertThat(executions).hasSize(3);
        for (Execution execution : executions) {
            if (execution.getParentId() == null) {
                assertThat(execution.getName()).isNull();
                assertThat(execution.getDescription()).isNull();

            } else if (execution.getParentId().equals(execution.getProcessInstanceId())) {
                assertThat(execution.getName()).isNull();
                assertThat(execution.getDescription()).isNull();
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).locale("es").listPage(0, 10);
        assertThat(executions).hasSize(3);
        for (Execution execution : executions) {
            if (execution.getParentId() == null) {
                assertThat(execution.getName()).isEqualTo("Nombre del proceso");
                assertThat(execution.getDescription()).isEqualTo("Descripción del proceso");

            } else if (execution.getParentId().equals(execution.getProcessInstanceId())) {
                assertThat(execution.getName()).isEqualTo("Nombre Subproceso");
                assertThat(execution.getDescription()).isEqualTo("Subproceso Descripción");
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).locale("it").listPage(0, 10);
        assertThat(executions).hasSize(3);
        for (Execution execution : executions) {
            if (execution.getParentId() == null) {
                assertThat(execution.getName()).isEqualTo("Nome del processo");
                assertThat(execution.getDescription()).isEqualTo("Descrizione del processo");

            } else if (execution.getParentId().equals(execution.getProcessInstanceId())) {
                assertThat(execution.getName()).isEqualTo("Nome sottoprocesso");
                assertThat(execution.getDescription()).isEqualTo("Sottoprocesso Descrizione");
            }
        }

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).locale("en-GB").listPage(0, 10);
        assertThat(executions).hasSize(3);
        for (Execution execution : executions) {
            if (execution.getParentId() == null) {
                assertThat(execution.getName()).isEqualTo("Process Name 'en-GB'");
                assertThat(execution.getDescription()).isEqualTo("Process Description 'en-GB'");

            } else if (execution.getParentId().equals(execution.getProcessInstanceId())) {
                assertThat(execution.getName()).isEqualTo("SubProcess Name 'en-GB'");
                assertThat(execution.getDescription()).isEqualTo("SubProcess Description 'en-GB'");
            }
        }

        Execution execution = runtimeService.createExecutionQuery().executionId(processInstance.getId()).singleResult();
        assertThat(execution.getName()).isNull();
        assertThat(execution.getDescription()).isNull();

        execution = runtimeService.createExecutionQuery().executionId(subProcessId).singleResult();
        assertThat(execution.getName()).isNull();
        assertThat(execution.getDescription()).isNull();

        execution = runtimeService.createExecutionQuery().executionId(processInstance.getId()).locale("es").singleResult();
        assertThat(execution.getName()).isEqualTo("Nombre del proceso");
        assertThat(execution.getDescription()).isEqualTo("Descripción del proceso");

        execution = runtimeService.createExecutionQuery().executionId(processInstance.getId()).locale("it").singleResult();
        assertThat(execution.getName()).isEqualTo("Nome del processo");
        assertThat(execution.getDescription()).isEqualTo("Descrizione del processo");

        execution = runtimeService.createExecutionQuery().executionId(subProcessId).locale("es").singleResult();
        assertThat(execution.getName()).isEqualTo("Nombre Subproceso");
        assertThat(execution.getDescription()).isEqualTo("Subproceso Descripción");

        execution = runtimeService.createExecutionQuery().executionId(subProcessId).locale("it").singleResult();
        assertThat(execution.getName()).isEqualTo("Nome sottoprocesso");
        assertThat(execution.getDescription()).isEqualTo("Sottoprocesso Descrizione");

        execution = runtimeService.createExecutionQuery().executionId(processInstance.getId()).locale("en-GB").singleResult();
        assertThat(execution.getName()).isEqualTo("Process Name 'en-GB'");
        assertThat(execution.getDescription()).isEqualTo("Process Description 'en-GB'");

        execution = runtimeService.createExecutionQuery().executionId(subProcessId).locale("en-GB").singleResult();
        assertThat(execution.getName()).isEqualTo("SubProcess Name 'en-GB'");
        assertThat(execution.getDescription()).isEqualTo("SubProcess Description 'en-GB'");

        execution = runtimeService.createExecutionQuery().executionId(processInstance.getId()).locale("en-AU").withLocalizationFallback().singleResult();
        assertThat(execution.getName()).isEqualTo("Process Name 'en'");
        assertThat(execution.getDescription()).isEqualTo("Process Description 'en'");

        execution = runtimeService.createExecutionQuery().executionId(subProcessId).locale("en-AU").withLocalizationFallback().singleResult();
        assertThat(execution.getName()).isEqualTo("SubProcess Name 'en'");
        assertThat(execution.getDescription()).isEqualTo("SubProcess Description 'en'");

        dynamicBpmnService.changeLocalizationName("en-US", "executionLocalization", "Process Name 'en-US'", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-US", "executionLocalization", "Process Description 'en-US'", infoNode);
        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        dynamicBpmnService.changeLocalizationName("en-US", "subProcess", "SubProcess Name 'en-US'", infoNode);
        dynamicBpmnService.changeLocalizationDescription("en-US", "subProcess", "SubProcess Description 'en-US'", infoNode);

        dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

        execution = runtimeService.createExecutionQuery().executionId(processInstance.getId()).locale("en-US").singleResult();
        assertThat(execution.getName()).isEqualTo("Process Name 'en-US'");
        assertThat(execution.getDescription()).isEqualTo("Process Description 'en-US'");

        execution = runtimeService.createExecutionQuery().executionId(subProcessId).locale("en-US").singleResult();
        assertThat(execution.getName()).isEqualTo("SubProcess Name 'en-US'");
        assertThat(execution.getDescription()).isEqualTo("SubProcess Description 'en-US'");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryStartedBefore() throws Exception {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date noon = calendar.getTime();

        processEngineConfiguration.getClock().setCurrentTime(noon);

        calendar.add(Calendar.HOUR_OF_DAY, 1);
        Date hourLater = calendar.getTime();

        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().startedBefore(hourLater).list();

        assertThat(executions).hasSize(2);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryStartedAfter() throws Exception {
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 2030);
        calendar.set(Calendar.MONTH, 8);
        calendar.set(Calendar.DAY_OF_MONTH, 30);
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date noon = calendar.getTime();

        processEngineConfiguration.getClock().setCurrentTime(noon);

        calendar.add(Calendar.HOUR_OF_DAY, -1);
        Date hourEarlier = calendar.getTime();

        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().startedAfter(hourEarlier).list();

        assertThat(executions).hasSize(2);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryStartedBy() throws Exception {
        final String authenticatedUser = "user1";
        identityService.setAuthenticatedUserId(authenticatedUser);
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        List<Execution> executions = runtimeService.createExecutionQuery().startedBy(authenticatedUser).list();

        assertThat(executions).hasSize(1);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/multipleSubProcess.bpmn20.xml",
            "org/flowable/engine/test/api/runtime/subProcess.bpmn20.xml" })
    public void testOnlySubProcessExecutions() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("multipleSubProcessTest");

        List<Execution> executions = runtimeService.createExecutionQuery().onlySubProcessExecutions().list();
        assertThat(executions).hasSize(2);
        for (Execution execution : executions) {
            if (execution.getParentId() == null) {
                assertThat(execution.getProcessInstanceId()).isNotSameAs(processInstance.getId());
            } else if (execution.getParentId().equals(execution.getProcessInstanceId())) {
                assertThat(execution.getActivityId()).isEqualTo("embeddedSubprocess");
            } else {
                fail("Unknown 'getParentID()'");
            }
        }
    }

    @Test
    public void testQueryVariableValueEqualsAndNotEquals() {
        ProcessInstance processWithStringValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With string value")
                .start();

        ProcessInstance processWithNullValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With null value")
                .start();

        ProcessInstance processWithLongValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With long value")
                .start();

        ProcessInstance processWithDoubleValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With double value")
                .start();

        Execution executionWithStringValue = runtimeService.createExecutionQuery()
                .processInstanceId(processWithStringValue.getId())
                .activityId("theTask")
                .singleResult();
        assertThat(executionWithStringValue).isNotNull();
        runtimeService.setVariableLocal(executionWithStringValue.getId(), "var", "TEST");

        Execution executionWithNullValue = runtimeService.createExecutionQuery()
                .processInstanceId(processWithNullValue.getId())
                .activityId("theTask")
                .singleResult();
        assertThat(executionWithNullValue).isNotNull();
        runtimeService.setVariableLocal(executionWithNullValue.getId(), "var", null);

        Execution executionWithLongValue = runtimeService.createExecutionQuery()
                .processInstanceId(processWithLongValue.getId())
                .activityId("theTask")
                .singleResult();
        assertThat(executionWithLongValue).isNotNull();
        runtimeService.setVariableLocal(executionWithLongValue.getId(), "var", 100L);

        Execution executionWithDoubleValue = runtimeService.createExecutionQuery()
                .processInstanceId(processWithDoubleValue.getId())
                .activityId("theTask")
                .singleResult();
        assertThat(executionWithDoubleValue).isNotNull();
        runtimeService.setVariableLocal(executionWithDoubleValue.getId(), "var", 45.55);

        assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("var", "TEST").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple(null, "theTask", executionWithNullValue.getId()),
                        tuple(null, "theTask", executionWithLongValue.getId()),
                        tuple(null, "theTask", executionWithDoubleValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().variableValueEquals("var", "TEST").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple(null, "theTask", executionWithStringValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("var", 100L).list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple(null, "theTask", executionWithStringValue.getId()),
                        tuple(null, "theTask", executionWithNullValue.getId()),
                        tuple(null, "theTask", executionWithDoubleValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().variableValueEquals("var", 100L).list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple(null, "theTask", executionWithLongValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("var", 45.55).list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple(null, "theTask", executionWithStringValue.getId()),
                        tuple(null, "theTask", executionWithNullValue.getId()),
                        tuple(null, "theTask", executionWithLongValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().variableValueEquals("var", 45.55).list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple(null, "theTask", executionWithDoubleValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().variableValueNotEquals("var", "test").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple(null, "theTask", executionWithStringValue.getId()),
                        tuple(null, "theTask", executionWithNullValue.getId()),
                        tuple(null, "theTask", executionWithLongValue.getId()),
                        tuple(null, "theTask", executionWithDoubleValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().variableValueNotEqualsIgnoreCase("var", "test").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple(null, "theTask", executionWithNullValue.getId()),
                        tuple(null, "theTask", executionWithLongValue.getId()),
                        tuple(null, "theTask", executionWithDoubleValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().variableValueEquals("var", "test").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .isEmpty();

        assertThat(runtimeService.createExecutionQuery().variableValueEqualsIgnoreCase("var", "test").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple(null, "theTask", executionWithStringValue.getId())
                );
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testQueryProcessVariableValueEqualsAndNotEquals() {
        ProcessInstance processWithStringValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With string value")
                .variable("var", "TEST")
                .start();

        ProcessInstance processWithNullValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With null value")
                .variable("var", null)
                .start();

        ProcessInstance processWithLongValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With long value")
                .variable("var", 100L)
                .start();

        ProcessInstance processWithDoubleValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With double value")
                .variable("var", 45.55)
                .start();

        Execution executionWithStringValue = runtimeService.createExecutionQuery()
                .processInstanceId(processWithStringValue.getId())
                .activityId("theTask")
                .singleResult();
        assertThat(executionWithStringValue).isNotNull();

        Execution executionWithNullValue = runtimeService.createExecutionQuery()
                .processInstanceId(processWithNullValue.getId())
                .activityId("theTask")
                .singleResult();
        assertThat(executionWithNullValue).isNotNull();

        Execution executionWithLongValue = runtimeService.createExecutionQuery()
                .processInstanceId(processWithLongValue.getId())
                .activityId("theTask")
                .singleResult();
        assertThat(executionWithLongValue).isNotNull();

        Execution executionWithDoubleValue = runtimeService.createExecutionQuery()
                .processInstanceId(processWithDoubleValue.getId())
                .activityId("theTask")
                .singleResult();
        assertThat(executionWithDoubleValue).isNotNull();

        assertThat(runtimeService.createExecutionQuery().processVariableValueNotEquals("var", "TEST").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", null, processWithNullValue.getId()),
                        tuple("With long value", null, processWithLongValue.getId()),
                        tuple("With double value", null, processWithDoubleValue.getId()),

                        tuple(null, "theTask", executionWithNullValue.getId()),
                        tuple(null, "theTask", executionWithLongValue.getId()),
                        tuple(null, "theTask", executionWithDoubleValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().processVariableValueEquals("var", "TEST").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", null, processWithStringValue.getId()),
                        tuple(null, "theTask", executionWithStringValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().processVariableValueNotEquals("var", 100L).list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", null, processWithStringValue.getId()),
                        tuple("With null value", null, processWithNullValue.getId()),
                        tuple("With double value", null, processWithDoubleValue.getId()),

                        tuple(null, "theTask", executionWithStringValue.getId()),
                        tuple(null, "theTask", executionWithNullValue.getId()),
                        tuple(null, "theTask", executionWithDoubleValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().processVariableValueEquals("var", 100L).list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple("With long value", null, processWithLongValue.getId()),
                        tuple(null, "theTask", executionWithLongValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().processVariableValueNotEquals("var", 45.55).list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", null, processWithStringValue.getId()),
                        tuple("With null value", null, processWithNullValue.getId()),
                        tuple("With long value", null, processWithLongValue.getId()),

                        tuple(null, "theTask", executionWithStringValue.getId()),
                        tuple(null, "theTask", executionWithNullValue.getId()),
                        tuple(null, "theTask", executionWithLongValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().processVariableValueEquals("var", 45.55).list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple("With double value", null, processWithDoubleValue.getId()),
                        tuple(null, "theTask", executionWithDoubleValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().processVariableValueNotEquals("var", "test").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", null, processWithStringValue.getId()),
                        tuple("With null value", null, processWithNullValue.getId()),
                        tuple("With long value", null, processWithLongValue.getId()),
                        tuple("With double value", null, processWithDoubleValue.getId()),

                        tuple(null, "theTask", executionWithStringValue.getId()),
                        tuple(null, "theTask", executionWithNullValue.getId()),
                        tuple(null, "theTask", executionWithLongValue.getId()),
                        tuple(null, "theTask", executionWithDoubleValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().processVariableValueNotEqualsIgnoreCase("var", "test").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple("With null value", null, processWithNullValue.getId()),
                        tuple("With long value", null, processWithLongValue.getId()),
                        tuple("With double value", null, processWithDoubleValue.getId()),

                        tuple(null, "theTask", executionWithNullValue.getId()),
                        tuple(null, "theTask", executionWithLongValue.getId()),
                        tuple(null, "theTask", executionWithDoubleValue.getId())
                );

        assertThat(runtimeService.createExecutionQuery().processVariableValueEquals("var", "test").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .isEmpty();

        assertThat(runtimeService.createExecutionQuery().processVariableValueEqualsIgnoreCase("var", "test").list())
                .extracting(Execution::getName, Execution::getActivityId, Execution::getId)
                .containsExactlyInAnyOrder(
                        tuple("With string value", null, processWithStringValue.getId()),
                        tuple(null, "theTask", executionWithStringValue.getId())
                );
    }
}
