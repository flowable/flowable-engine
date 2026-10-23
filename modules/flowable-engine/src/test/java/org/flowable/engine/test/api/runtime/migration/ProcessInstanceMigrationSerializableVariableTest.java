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
package org.flowable.engine.test.api.runtime.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricDetail;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.migration.Script;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.impl.CustomConfigurationFlowableTestCase;
import org.flowable.variable.service.impl.types.SerializableVariableTypeMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ArrayNode;

/**
 * Tests for migrating process instances with serializable variables into JSON variables.
 * Serializable variables are disabled by default, so they are explicitly enabled for these tests.
 */
public class ProcessInstanceMigrationSerializableVariableTest extends CustomConfigurationFlowableTestCase {

    public ProcessInstanceMigrationSerializableVariableTest() {
        super(ProcessInstanceMigrationSerializableVariableTest.class.getSimpleName());
    }

    @Override
    protected void configureConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        processEngineConfiguration.setSerializableVariableTypeMode(SerializableVariableTypeMode.ENABLED);
    }

    @AfterEach
    protected void tearDown() {
        deleteDeployments();
    }

    @Test
    public void preUpgradeScriptMigration() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP", Collections.singletonMap("listVariable", new ArrayList()));
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions().singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder().preUpgradeScript(new Script("groovy",
                        """
                                import tools.jackson.databind.ObjectMapper
                                import tools.jackson.databind.node.ArrayNode
                                import org.flowable.engine.impl.context.Context

                                List<String> list  = execution.getVariable('listVariable')

                                ObjectMapper mapper = Context.getProcessEngineConfiguration().getObjectMapper()

                                ArrayNode jsonArray = mapper.createArrayNode()
                                list.each {jsonArray.add(it)}

                                execution.setVariable("listVariable", jsonArray)"""))
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // serializable - when starting the process instance
            // serializable - when triggering the received task (the value is updated so new detail is created)
            // json - when doing the migration new variable is set
            // json - when triggering the received task (the value is updated so new details is created 
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "json", "json");
        }
    }

    @Test
    public void preUpgradeJavaDelegate() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP", Collections.singletonMap("listVariable", new ArrayList()));
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions().singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder()
                .preUpgradeJavaDelegate("org.flowable.engine.test.api.runtime.migration.ConvertProcessVariable")
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // serializable - when starting the process instance
            // serializable - when triggering the received task (the value is updated so new detail is created)
            // json - when doing the migration new variable is set
            // json - when triggering the received task (the value is updated so new details is created
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "json", "json");
        }
    }

    @Test
    public void preUpgradeJavaDelegateExpression() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        Map<String, Object> variables = new HashMap<>();
        variables.put("listVariable", new ArrayList());
        variables.put("convertProcessVariable", new ConvertProcessVariable()); // using instead of beans. Not nice, but it uses similar resolver as beans
        ProcessInstance processInstanceToMigrate = runtimeService
                .startProcessInstanceByKey("MP", variables);
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions()
                .singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder()
                .preUpgradeJavaDelegateExpression("${convertProcessVariable}")
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // serializable - when starting the process instance (listVariable)
            // serializable - when starting the process instance (convertProcessVariable)
            // serializable - when triggering the received task (the value is updated so new detail is created)
            // json - when doing the migration new variable is set
            // json - when triggering the received task (the value is updated so new details is created
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "serializable", "json", "json");
        }
    }

    @Test
    public void postUpgradeScriptMigration() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        //Start and instance of the recent first version of the process for migration and one for reference
        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP", Collections.singletonMap("listVariable", new ArrayList()));
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions().singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder().postUpgradeScript(new Script("groovy",
                        """
                                import tools.jackson.databind.ObjectMapper
                                import tools.jackson.databind.node.ArrayNode
                                import org.flowable.engine.impl.context.Context

                                List<String> list  = execution.getVariable('listVariable')

                                ObjectMapper mapper = Context.getProcessEngineConfiguration().getObjectMapper()

                                ArrayNode jsonArray = mapper.createArrayNode()
                                list.each {jsonArray.add(it)}

                                execution.setVariable("listVariable", jsonArray)"""))
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // serializable - when starting the process instance
            // serializable - when triggering the received task (the value is updated so new detail is created)
            // json - when doing the migration new variable is set
            // json - when triggering the received task (the value is updated so new details is created
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "json", "json");
        }
    }

    @Test
    public void postUpgradeJavaDelegate() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        ProcessInstance processInstanceToMigrate = runtimeService.startProcessInstanceByKey("MP", Collections.singletonMap("listVariable", new ArrayList()));
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions().singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder()
                .postUpgradeJavaDelegate("org.flowable.engine.test.api.runtime.migration.ConvertProcessVariable")
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            // serializable - when starting the process instance
            // serializable - when triggering the received task (the value is updated so new detail is created)
            // json - when doing the migration new variable is set
            // json - when triggering the received task (the value is updated so new details is created
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "json", "json");
        }
    }

    @Test
    public void postUpgradeJavaDelegateExpression() {
        //Deploy first version of the process
        deployProcessDefinition("my deploy", "org/flowable/engine/test/api/runtime/migration/serializable-variable-process.bpmn20.xml");

        Map<String, Object> variables = new HashMap<>();
        variables.put("listVariable", new ArrayList());
        variables.put("convertProcessVariable", new ConvertProcessVariable()); // using instead of beans. Not nice, but it uses similar resolver as beans
        ProcessInstance processInstanceToMigrate = runtimeService
                .startProcessInstanceByKey("MP", variables);
        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstanceToMigrate.getId()).onlyChildExecutions()
                .singleResult();
        runtimeService.trigger(execution.getId());

        assertThat((List) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable")).contains("new value");

        ProcessDefinition targetProcessDefinition = deployProcessDefinition("my deploy",
                "org/flowable/engine/test/api/runtime/migration/json-variable-process.bpmn20.xml");

        processMigrationService.createProcessInstanceMigrationBuilder()
                .postUpgradeJavaDelegateExpression("${convertProcessVariable}")
                .migrateToProcessDefinition(targetProcessDefinition.getId())
                .migrate(processInstanceToMigrate.getId());

        assertThatProcessVariableConverted(processInstanceToMigrate, execution);

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.FULL, processEngineConfiguration)) {
            assertThatVariablesTypeHistoryIs(processInstanceToMigrate, "serializable", "serializable", "serializable", "json", "json");
        }

    }

    private void assertThatVariablesTypeHistoryIs(ProcessInstance processInstanceToMigrate, String... values) {
        List<HistoricDetail> updateList = historyService.createHistoricDetailQuery().processInstanceId(processInstanceToMigrate.getId()).variableUpdates()
                .list();

        updateList.sort(Comparator.comparingInt(histDetail -> Integer.parseInt(histDetail.getId())));
        assertThat(updateList)
                .extracting(historicDetail -> ((HistoricDetailVariableInstanceUpdateEntity) historicDetail).getVariableType().getTypeName())
                .containsExactly(values);
    }

    private void assertThatProcessVariableConverted(ProcessInstance processInstanceToMigrate, Execution execution) {
        assertThat((ArrayNode) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable"))
                .extracting(jsonNode -> jsonNode.asString())
                .containsExactly("new value");

        runtimeService.trigger(execution.getId());

        assertThat((ArrayNode) runtimeService.getVariable(processInstanceToMigrate.getId(), "listVariable"))
                .extracting(jsonNode -> jsonNode.asString())
                .containsExactly("new value", "new value 2");

        runtimeService.trigger(execution.getId());

        assertProcessEnded(processInstanceToMigrate.getId());
    }
}
