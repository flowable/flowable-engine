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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.api.variables.SerializableVariableTest.TestSerializableVariable;
import org.flowable.job.api.FlowableUnrecoverableJobException;
import org.flowable.job.api.Job;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.types.LongStringType;
import org.flowable.variable.service.impl.types.SerializableType;
import org.flowable.variable.service.impl.types.SerializableVariableTypeMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Filip Hrisafov
 */
class SerializableVariableTypeModeTest {

    protected static final String JDBC_URL = "jdbc:h2:mem:serializableVariableTypeModeTest";

    protected List<ProcessEngine> processEngines = new ArrayList<>();

    // Process engine with serializable variables enabled, used for creating the serializable data
    protected ProcessEngine enabledProcessEngine;
    protected ProcessInstance processInstance;

    @BeforeEach
    void setUp() {
        enabledProcessEngine = buildProcessEngine(SerializableVariableTypeMode.ENABLED, "create-drop");
        enabledProcessEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
                .deploy();

        processInstance = enabledProcessEngine.getRuntimeService().createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .variable("serializableVar", new TestSerializableVariable(10))
                .start();
    }

    @AfterEach
    void tearDown() {
        // Close in reverse order, the first engine is the one that drops the schema
        for (int i = processEngines.size() - 1; i >= 0; i--) {
            processEngines.get(i).close();
        }
    }

    @Test
    void defaultModeIsDisabled() {
        assertThat(new StandaloneInMemProcessEngineConfiguration().getSerializableVariableTypeMode())
                .isEqualTo(SerializableVariableTypeMode.DISABLED);
    }

    @Test
    void enabledMode() {
        RuntimeService runtimeService = enabledProcessEngine.getRuntimeService();
        assertThat(runtimeService.getVariable(processInstance.getId(), "serializableVar", TestSerializableVariable.class))
                .extracting(TestSerializableVariable::getNumber)
                .isEqualTo(10);

        runtimeService.setVariable(processInstance.getId(), "serializableVar", new TestSerializableVariable(20));
        assertThat(runtimeService.getVariable(processInstance.getId(), "serializableVar", TestSerializableVariable.class))
                .extracting(TestSerializableVariable::getNumber)
                .isEqualTo(20);
    }

    @Test
    void readOnlyMode() {
        RuntimeService runtimeService = buildProcessEngine(SerializableVariableTypeMode.READ_ONLY, "false").getRuntimeService();

        assertThat(runtimeService.getVariable(processInstance.getId(), "serializableVar", TestSerializableVariable.class))
                .extracting(TestSerializableVariable::getNumber)
                .isEqualTo(10);

        assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "serializableVar", new TestSerializableVariable(20)))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Storing the value of variable 'serializableVar' of type " + TestSerializableVariable.class.getName()
                        + " is not allowed, because serializable variables are disabled");

        assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "otherVar", new TestSerializableVariable(30)))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Storing the value of variable 'otherVar' of type " + TestSerializableVariable.class.getName()
                        + " is not allowed, because serializable variables are disabled");

        assertThat(runtimeService.getVariable(processInstance.getId(), "serializableVar", TestSerializableVariable.class))
                .extracting(TestSerializableVariable::getNumber)
                .isEqualTo(10);

        // Migrating the existing variable to another type is possible
        runtimeService.setVariable(processInstance.getId(), "serializableVar", "migrated");
        VariableInstance variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "serializableVar");
        assertThat(variableInstance.getTypeName()).isEqualTo("string");
        assertThat(variableInstance.getValue()).isEqualTo("migrated");
    }

    @Test
    void readOnlyModeDoesNotUpdateTrackedObjects() {
        ProcessEngine readOnlyProcessEngine = buildProcessEngine(SerializableVariableTypeMode.READ_ONLY, "false");

        readOnlyProcessEngine.getManagementService().executeCommand(commandContext -> {
            TestSerializableVariable value = (TestSerializableVariable) readOnlyProcessEngine.getRuntimeService()
                    .getVariable(processInstance.getId(), "serializableVar");
            value.setNumber(20);
            return null;
        });

        assertThat(enabledProcessEngine.getRuntimeService().getVariable(processInstance.getId(), "serializableVar", TestSerializableVariable.class))
                .extracting(TestSerializableVariable::getNumber)
                .isEqualTo(10);
    }

    @Test
    void disabledMode() {
        ProcessEngine disabledProcessEngine = buildProcessEngine(SerializableVariableTypeMode.DISABLED, "false");
        RuntimeService runtimeService = disabledProcessEngine.getRuntimeService();

        assertThatThrownBy(() -> runtimeService.getVariable(processInstance.getId(), "serializableVar"))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Reading the value of serializable variable 'serializableVar' is not allowed, because serializable variables are disabled");

        assertThatThrownBy(() -> runtimeService.setVariable(processInstance.getId(), "otherVar", new TestSerializableVariable(30)))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Storing the value of variable 'otherVar' of type " + TestSerializableVariable.class.getName()
                        + " is not allowed, because serializable variables are disabled");

        // The variable instance can still be loaded without accessing the value
        assertThat(runtimeService.createVariableInstanceQuery().processInstanceId(processInstance.getId()).excludeVariableInitialization().list())
                .extracting(VariableInstance::getName, VariableInstance::getTypeName)
                .containsExactly(tuple("serializableVar", SerializableType.TYPE_NAME));

        // Removing the variable and deleting the process instance is possible
        runtimeService.removeVariable(processInstance.getId(), "serializableVar");
        assertThat(runtimeService.hasVariable(processInstance.getId(), "serializableVar")).isFalse();

        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    void disabledModeFailsJobWithoutRetries() {
        ProcessEngine disabledProcessEngine = buildProcessEngine(SerializableVariableTypeMode.DISABLED, "false");
        disabledProcessEngine.getRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/engine/test/api/variables/SerializableVariableTypeModeTest.asyncSerializableVariable.bpmn20.xml")
                .deploy();

        ProcessInstance asyncProcessInstance = disabledProcessEngine.getRuntimeService().startProcessInstanceByKey("asyncSerializableVariable");
        ManagementService managementService = disabledProcessEngine.getManagementService();
        Job job = managementService.createJobQuery().processInstanceId(asyncProcessInstance.getId()).singleResult();

        assertThatThrownBy(() -> managementService.executeJob(job.getId()))
                .isInstanceOf(FlowableUnrecoverableJobException.class)
                .hasMessageContaining("Storing the value of variable 'serializableVar'");

        // Retrying the job would not help, so it is moved to the dead letter jobs right away
        assertThat(managementService.createJobQuery().processInstanceId(asyncProcessInstance.getId()).count()).isZero();
        assertThat(managementService.createDeadLetterJobQuery().processInstanceId(asyncProcessInstance.getId()).singleResult())
                .extracting(Job::getRetries)
                .isEqualTo(0);
    }

    @Test
    void disabledModeProcessInstanceCanBeDeleted() {
        ProcessEngine disabledProcessEngine = buildProcessEngine(SerializableVariableTypeMode.DISABLED, "false");
        RuntimeService runtimeService = disabledProcessEngine.getRuntimeService();

        runtimeService.deleteProcessInstance(processInstance.getId(), "test");
        assertThat(runtimeService.createProcessInstanceQuery().processInstanceId(processInstance.getId()).count()).isZero();
    }

    @Test
    void longStringVariablesWorkWhenSerializableIsDisabled() {
        RuntimeService runtimeService = buildProcessEngine(SerializableVariableTypeMode.DISABLED, "false").getRuntimeService();

        String longString = "a".repeat(5000);
        runtimeService.setVariable(processInstance.getId(), "longStringVar", longString);

        VariableInstance variableInstance = runtimeService.getVariableInstance(processInstance.getId(), "longStringVar");
        assertThat(variableInstance.getTypeName()).isEqualTo(LongStringType.TYPE_NAME);
        assertThat(variableInstance.getValue()).isEqualTo(longString);
    }

    @Test
    void longStringVariableDoesNotDeserializeNonStringValues() throws Exception {
        String longString = "a".repeat(5000);
        RuntimeService runtimeService = enabledProcessEngine.getRuntimeService();
        runtimeService.setVariable(processInstance.getId(), "longStringVar", longString);

        // Simulate tampered bytes in the database
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream outputStream = new ObjectOutputStream(buffer)) {
            outputStream.writeObject(new TestSerializableVariable(10));
        }
        enabledProcessEngine.getManagementService().executeCommand(commandContext -> {
            VariableInstanceEntity variableInstance = (VariableInstanceEntity) runtimeService.getVariableInstance(processInstance.getId(), "longStringVar");
            variableInstance.setBytes(buffer.toByteArray());
            return null;
        });

        RuntimeService otherRuntimeService = buildProcessEngine(SerializableVariableTypeMode.ENABLED, "false").getRuntimeService();
        assertThatThrownBy(() -> otherRuntimeService.getVariable(processInstance.getId(), "longStringVar"))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("Couldn't deserialize object in variable 'longStringVar'")
                .hasRootCauseInstanceOf(InvalidClassException.class);
    }

    protected ProcessEngine buildProcessEngine(SerializableVariableTypeMode mode, String databaseSchemaUpdate) {
        ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) new StandaloneInMemProcessEngineConfiguration()
                .setJdbcUrl(JDBC_URL)
                .setDatabaseSchemaUpdate(databaseSchemaUpdate)
                .setEngineName("serializableVariableTypeModeTest" + processEngines.size());
        processEngineConfiguration.setSerializableVariableTypeMode(mode);
        ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();
        processEngines.add(processEngine);
        return processEngine;
    }

    public static class SetSerializableVariableDelegate implements JavaDelegate {

        @Override
        public void execute(DelegateExecution execution) {
            execution.setVariable("serializableVar", new TestSerializableVariable(10));
        }
    }
}
