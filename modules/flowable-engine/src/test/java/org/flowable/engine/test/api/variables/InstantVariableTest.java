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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.types.InstantType;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class InstantVariableTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testGetInstantVariable() {
        Instant nowInstant = Instant.now();
        Instant nowInstantWithoutNanos = nowInstant.truncatedTo(ChronoUnit.MILLIS);
        Instant oneYearBefore = nowInstant.minus(365, ChronoUnit.DAYS);
        Instant oneYearBeforeWithoutNanos = oneYearBefore.truncatedTo(ChronoUnit.MILLIS);
        Instant oneYearLater = nowInstant.plus(365, ChronoUnit.DAYS);
        Instant oneYearLaterWithoutNanos = oneYearLater.truncatedTo(ChronoUnit.MILLIS);
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("nowInstant", nowInstant)
                .variable("oneYearBefore", oneYearBefore)
                .variable("oneYearLater", oneYearLater)
                .start();

        VariableInstance nowInstantVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "nowInstant");
        assertThat(nowInstantVariableInstance.getTypeName()).isEqualTo(InstantType.TYPE_NAME);
        assertThat(nowInstantVariableInstance.getValue()).isEqualTo(nowInstantWithoutNanos);

        VariableInstance oneYearBeforeVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "oneYearBefore");
        assertThat(oneYearBeforeVariableInstance.getTypeName()).isEqualTo(InstantType.TYPE_NAME);
        assertThat(oneYearBeforeVariableInstance.getValue()).isEqualTo(oneYearBeforeWithoutNanos);

        VariableInstance oneYearLaterVariableInstance = runtimeService.getVariableInstance(processInstance.getId(), "oneYearLater");
        assertThat(oneYearLaterVariableInstance.getTypeName()).isEqualTo(InstantType.TYPE_NAME);
        assertThat(oneYearLaterVariableInstance.getValue()).isEqualTo(oneYearLaterWithoutNanos);

        assertThat(runtimeService.getVariables(processInstance.getId()))
                .containsOnly(
                        entry("nowInstant", nowInstantWithoutNanos),
                        entry("oneYearBefore", oneYearBeforeWithoutNanos),
                        entry("oneYearLater", oneYearLaterWithoutNanos)
                );
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void testGetInstantVariableFromTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();

        Map<String, Object> variables = new HashMap<>();
        Instant nowInstant = Instant.now();
        Instant nowInstantWithoutNanos = nowInstant.truncatedTo(ChronoUnit.MILLIS);
        Instant oneYearLater = nowInstant.plus(365, ChronoUnit.DAYS);
        Instant oneYearLaterWithoutNanos = oneYearLater.truncatedTo(ChronoUnit.MILLIS);
        variables.put("nowInstant", nowInstant);
        variables.put("oneYearLater", oneYearLater);
        Task task = taskService.createTaskQuery().singleResult();
        taskService.setVariables(task.getId(), variables);

        VariableInstance nowInstantVariableInstance = taskService.getVariableInstance(task.getId(), "nowInstant");
        assertThat(nowInstantVariableInstance.getTypeName()).isEqualTo(InstantType.TYPE_NAME);
        assertThat(nowInstantVariableInstance.getValue()).isEqualTo(nowInstantWithoutNanos);

        VariableInstance oneYearLaterVariableInstance = taskService.getVariableInstance(task.getId(), "oneYearLater");
        assertThat(oneYearLaterVariableInstance.getTypeName()).isEqualTo(InstantType.TYPE_NAME);
        assertThat(oneYearLaterVariableInstance.getValue()).isEqualTo(oneYearLaterWithoutNanos);

        assertThat(taskService.getVariables(task.getId()))
                .containsOnly(
                        entry("nowInstant", nowInstantWithoutNanos),
                        entry("oneYearLater", oneYearLaterWithoutNanos)
                );
    }
}
