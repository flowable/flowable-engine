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
package org.flowable.engine.test.api.variables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.types.LocalDateTimeType;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class LocalDateTimeVariableTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testGetLocalDateTimeVariable() {
        LocalDateTime nowLocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime oneYearBefore = nowLocalDateTime.minusYears(1);
        LocalDateTime oneYearLater = nowLocalDateTime.plusYears(1);
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("nowLocalDateTime", nowLocalDateTime)
                .variable("oneYearBefore", oneYearBefore)
                .variable("oneYearLater", oneYearLater)
                .start();

        VariableInstance nowLocalDateTimeVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "nowLocalDateTime");
        assertThat(nowLocalDateTimeVariableInstance.getTypeName()).isEqualTo(LocalDateTimeType.TYPE_NAME);
        assertThat(nowLocalDateTimeVariableInstance.getValue()).isEqualTo(nowLocalDateTime);

        VariableInstance oneYearBeforeVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "oneYearBefore");
        assertThat(oneYearBeforeVariableInstance.getTypeName()).isEqualTo(LocalDateTimeType.TYPE_NAME);
        assertThat(oneYearBeforeVariableInstance.getValue()).isEqualTo(oneYearBefore);

        VariableInstance oneYearLaterVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "oneYearLater");
        assertThat(oneYearLaterVariableInstance.getTypeName()).isEqualTo(LocalDateTimeType.TYPE_NAME);
        assertThat(oneYearLaterVariableInstance.getValue()).isEqualTo(oneYearLater);

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("nowLocalDateTime", nowLocalDateTime),
                        entry("oneYearBefore", oneYearBefore),
                        entry("oneYearLater", oneYearLater)
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testGetLocalDateTimeVariableFromTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();

        Map<String, Object> variables = new HashMap<>();
        LocalDateTime nowLocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime oneYearLater = nowLocalDateTime.plusYears(1);
        variables.put("nowLocalDateTime", nowLocalDateTime);
        variables.put("oneYearLater", oneYearLater);
        Task task = taskService.createTaskQuery().singleResult();
        taskService.setVariables(task.getId(), variables);

        VariableInstance nowLocalDateTimeVariableInstance = taskService.getVariableInstance(task.getId(), "nowLocalDateTime");
        assertThat(nowLocalDateTimeVariableInstance.getTypeName()).isEqualTo(LocalDateTimeType.TYPE_NAME);
        assertThat(nowLocalDateTimeVariableInstance.getValue()).isEqualTo(nowLocalDateTime);

        VariableInstance oneYearLaterVariableInstance = taskService.getVariableInstance(task.getId(), "oneYearLater");
        assertThat(oneYearLaterVariableInstance.getTypeName()).isEqualTo(LocalDateTimeType.TYPE_NAME);
        assertThat(oneYearLaterVariableInstance.getValue()).isEqualTo(oneYearLater);

        assertThat(taskService.getVariables(task.getId()))
                .containsOnly(
                        entry("nowLocalDateTime", nowLocalDateTime),
                        entry("oneYearLater", oneYearLater)
                );
    }
}
