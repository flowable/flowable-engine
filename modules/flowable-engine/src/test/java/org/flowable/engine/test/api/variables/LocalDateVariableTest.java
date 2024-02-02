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

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.types.LocalDateType;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class LocalDateVariableTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testGetLocalDateVariable() {
        LocalDate nowLocalDate = LocalDate.now();
        LocalDate oneYearBefore = nowLocalDate.minusYears(1);
        LocalDate oneYearLater = nowLocalDate.plusYears(1);
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("nowLocalDate", nowLocalDate)
                .variable("oneYearBefore", oneYearBefore)
                .variable("oneYearLater", oneYearLater)
                .start();

        VariableInstance nowLocalDateVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "nowLocalDate");
        assertThat(nowLocalDateVariableInstance.getTypeName()).isEqualTo(LocalDateType.TYPE_NAME);
        assertThat(nowLocalDateVariableInstance.getValue()).isEqualTo(nowLocalDate);

        VariableInstance oneYearBeforeVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "oneYearBefore");
        assertThat(oneYearBeforeVariableInstance.getTypeName()).isEqualTo(LocalDateType.TYPE_NAME);
        assertThat(oneYearBeforeVariableInstance.getValue()).isEqualTo(oneYearBefore);

        VariableInstance oneYearLaterVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "oneYearLater");
        assertThat(oneYearLaterVariableInstance.getTypeName()).isEqualTo(LocalDateType.TYPE_NAME);
        assertThat(oneYearLaterVariableInstance.getValue()).isEqualTo(oneYearLater);

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("nowLocalDate", nowLocalDate),
                        entry("oneYearBefore", oneYearBefore),
                        entry("oneYearLater", oneYearLater)
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testGetLocalDateVariableFromTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();

        Map<String, Object> variables = new HashMap<>();
        LocalDate nowLocalDate = LocalDate.now();
        LocalDate oneYearLater = nowLocalDate.plusYears(1);
        variables.put("nowLocalDate", nowLocalDate);
        variables.put("oneYearLater", oneYearLater);
        Task task = taskService.createTaskQuery().singleResult();
        taskService.setVariables(task.getId(), variables);

        VariableInstance nowLocalDateVariableInstance = taskService.getVariableInstance(task.getId(), "nowLocalDate");
        assertThat(nowLocalDateVariableInstance.getTypeName()).isEqualTo(LocalDateType.TYPE_NAME);
        assertThat(nowLocalDateVariableInstance.getValue()).isEqualTo(nowLocalDate);

        VariableInstance oneYearLaterVariableInstance = taskService.getVariableInstance(task.getId(), "oneYearLater");
        assertThat(oneYearLaterVariableInstance.getTypeName()).isEqualTo(LocalDateType.TYPE_NAME);
        assertThat(oneYearLaterVariableInstance.getValue()).isEqualTo(oneYearLater);

        assertThat(taskService.getVariables(task.getId()))
                .containsOnly(
                        entry("nowLocalDate", nowLocalDate),
                        entry("oneYearLater", oneYearLater)
                );
    }
}
