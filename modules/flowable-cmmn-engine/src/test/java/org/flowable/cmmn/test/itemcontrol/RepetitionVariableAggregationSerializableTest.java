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
package org.flowable.cmmn.test.itemcontrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.test.EngineConfigurer;
import org.flowable.cmmn.test.impl.CustomCmmnConfigurationFlowableTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.task.api.Task;
import org.flowable.variable.service.impl.types.SerializableVariableTypeMode;
import org.junit.jupiter.api.Test;

/**
 * Tests for the variable aggregation of repeating plan items with serializable variables.
 * These need an engine with serializable variables enabled.
 *
 * @author Filip Hrisafov
 */
public class RepetitionVariableAggregationSerializableTest extends CustomCmmnConfigurationFlowableTestCase {

    @EngineConfigurer
    protected static void configureConfiguration(CmmnEngineConfiguration cmmnEngineConfiguration) {
        cmmnEngineConfiguration.setSerializableVariableTypeMode(SerializableVariableTypeMode.ENABLED);
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/itemcontrol/RepetitionVariableAggregationTest.testParallelRepeatingUserTaskVariableTypes.cmmn")
    public void testParallelRepeatingUserTaskVariableTypesUnsupportedVariableTypes() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("repeatingTask")
                .variable("otherVariable", "Hello World")
                .variable("myCollection", Arrays.asList("a", "b"))
                .start();

        Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Task A").singleResult();
        cmmnTaskService.complete(task.getId());

        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).taskName("Task B")
                .orderByTaskPriority().asc()
                .list();
        assertThat(tasks).hasSize(2);

        Map<String, Object> variables = new HashMap<>();
        variables.put("description", "Description for task with LocalDate");
        variables.put("score", 10);
        variables.put("passed", true);
        variables.put("location", "Springfield");
        variables.put("startTime", new TestSerializableVariable());

        assertThatThrownBy(() -> cmmnTaskService.complete(tasks.get(0).getId(), variables))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("Cannot aggregate variable: ")
                .hasMessageContaining("startTime");
    }

    public static class TestSerializableVariable implements Serializable {

        private static final long serialVersionUID = 1L;

        private String someField;

        public String getSomeField() {
            return someField;
        }

        public void setSomeField(String someField) {
            this.someField = someField;
        }
    }

}
