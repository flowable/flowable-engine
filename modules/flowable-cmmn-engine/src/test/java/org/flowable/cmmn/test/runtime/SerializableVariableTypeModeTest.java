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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cfg.StandaloneInMemCmmnEngineConfiguration;
import org.flowable.common.engine.impl.logging.LoggingListener;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;
import org.flowable.job.api.FlowableUnrecoverableJobException;
import org.flowable.job.api.Job;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.impl.types.SerializableType;
import org.flowable.variable.service.impl.types.SerializableVariableTypeMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ObjectNode;

/**
 * @author Filip Hrisafov
 */
class SerializableVariableTypeModeTest {

    protected static final String JDBC_URL = "jdbc:h2:mem:cmmnSerializableVariableTypeModeTest";

    protected List<CmmnEngine> cmmnEngines = new ArrayList<>();

    @AfterEach
    void tearDown() {
        // Close in reverse order, the first engine is the one that drops the schema
        for (int i = cmmnEngines.size() - 1; i >= 0; i--) {
            cmmnEngines.get(i).close();
        }
    }

    @Test
    void defaultModeIsDisabled() {
        assertThat(new StandaloneInMemCmmnEngineConfiguration().getSerializableVariableTypeMode())
                .isEqualTo(SerializableVariableTypeMode.DISABLED);
    }

    @Test
    void enabledMode() {
        CmmnEngine enabledCmmnEngine = buildCmmnEngine(SerializableVariableTypeMode.ENABLED);
        CaseInstance caseInstance = startCaseWithSerializableVariable(enabledCmmnEngine);
        CmmnRuntimeService runtimeService = enabledCmmnEngine.getCmmnRuntimeService();

        assertThat(runtimeService.getVariable(caseInstance.getId(), "serializableVar"))
                .isEqualTo(new TestSerializableVariable(10));

        runtimeService.setVariable(caseInstance.getId(), "serializableVar", new TestSerializableVariable(20));
        assertThat(runtimeService.getVariable(caseInstance.getId(), "serializableVar"))
                .isEqualTo(new TestSerializableVariable(20));
    }

    @Test
    void readOnlyMode() {
        CmmnEngine enabledCmmnEngine = buildCmmnEngine(SerializableVariableTypeMode.ENABLED);
        CaseInstance caseInstance = startCaseWithSerializableVariable(enabledCmmnEngine);
        CmmnRuntimeService runtimeService = buildCmmnEngine(SerializableVariableTypeMode.READ_ONLY).getCmmnRuntimeService();

        assertThat(runtimeService.getVariable(caseInstance.getId(), "serializableVar"))
                .isEqualTo(new TestSerializableVariable(10));

        assertThatThrownBy(() -> runtimeService.setVariable(caseInstance.getId(), "serializableVar", new TestSerializableVariable(20)))
                .isExactlyInstanceOf(FlowableUnrecoverableJobException.class)
                .hasMessageContaining("Storing the value of variable 'serializableVar' of type " + TestSerializableVariable.class.getName()
                        + " is not allowed");

        assertThatThrownBy(() -> runtimeService.setVariable(caseInstance.getId(), "otherVar", new TestSerializableVariable(30)))
                .isExactlyInstanceOf(FlowableUnrecoverableJobException.class)
                .hasMessageContaining("Storing the value of variable 'otherVar' of type " + TestSerializableVariable.class.getName()
                        + " is not allowed");

        assertThat(runtimeService.getVariable(caseInstance.getId(), "serializableVar"))
                .isEqualTo(new TestSerializableVariable(10));

        // Migrating the existing variable to another type is possible
        runtimeService.setVariable(caseInstance.getId(), "serializableVar", "migrated");
        VariableInstance variableInstance = runtimeService.getVariableInstance(caseInstance.getId(), "serializableVar");
        assertThat(variableInstance.getTypeName()).isEqualTo("string");
        assertThat(variableInstance.getValue()).isEqualTo("migrated");
    }

    @Test
    void readOnlyModeDoesNotUpdateTrackedObjects() {
        CmmnEngine enabledCmmnEngine = buildCmmnEngine(SerializableVariableTypeMode.ENABLED);
        CaseInstance caseInstance = startCaseWithSerializableVariable(enabledCmmnEngine);
        CmmnEngine readOnlyCmmnEngine = buildCmmnEngine(SerializableVariableTypeMode.READ_ONLY);

        readOnlyCmmnEngine.getCmmnEngineConfiguration().getCommandExecutor().execute(commandContext -> {
            TestSerializableVariable value = (TestSerializableVariable) readOnlyCmmnEngine.getCmmnRuntimeService()
                    .getVariable(caseInstance.getId(), "serializableVar");
            value.setNumber(20);
            return null;
        });

        assertThat(enabledCmmnEngine.getCmmnRuntimeService().getVariable(caseInstance.getId(), "serializableVar"))
                .isEqualTo(new TestSerializableVariable(10));
    }

    @Test
    void disabledMode() {
        CmmnEngine enabledCmmnEngine = buildCmmnEngine(SerializableVariableTypeMode.ENABLED);
        CaseInstance caseInstance = startCaseWithSerializableVariable(enabledCmmnEngine);
        CmmnEngine disabledCmmnEngine = buildCmmnEngine(SerializableVariableTypeMode.DISABLED);
        CmmnRuntimeService runtimeService = disabledCmmnEngine.getCmmnRuntimeService();

        assertThatThrownBy(() -> runtimeService.getVariable(caseInstance.getId(), "serializableVar"))
                .isExactlyInstanceOf(FlowableUnrecoverableJobException.class)
                .hasMessageContaining("Reading the value of serializable variable 'serializableVar' is not allowed, because serializable variables are disabled");

        assertThatThrownBy(() -> runtimeService.setVariable(caseInstance.getId(), "otherVar", new TestSerializableVariable(30)))
                .isExactlyInstanceOf(FlowableUnrecoverableJobException.class)
                .hasMessageContaining("Storing the value of variable 'otherVar' of type " + TestSerializableVariable.class.getName()
                        + " is not allowed, because serializable variables are disabled");

        // The variable instance can still be loaded without accessing the value
        assertThat(runtimeService.createVariableInstanceQuery().caseInstanceId(caseInstance.getId()).excludeVariableInitialization().list())
                .extracting(VariableInstance::getName, VariableInstance::getTypeName)
                .containsExactly(tuple("serializableVar", SerializableType.TYPE_NAME));

        // Removing the variable is possible
        runtimeService.removeVariable(caseInstance.getId(), "serializableVar");
        assertThat(runtimeService.hasVariable(caseInstance.getId(), "serializableVar")).isFalse();
    }

    @Test
    void disabledModeVariableCanBeOverwrittenWithAnotherType() {
        CmmnEngine enabledCmmnEngine = buildCmmnEngine(SerializableVariableTypeMode.ENABLED);
        CaseInstance caseInstance = startCaseWithSerializableVariable(enabledCmmnEngine);
        List<ObjectNode> loggingNodes = new ArrayList<>();
        CmmnRuntimeService runtimeService = buildCmmnEngine(SerializableVariableTypeMode.DISABLED, loggingNodes::addAll).getCmmnRuntimeService();

        // The existing value does not need to be read for replacing it with a value of another type
        runtimeService.setVariable(caseInstance.getId(), "serializableVar", "migrated");

        VariableInstance variableInstance = runtimeService.getVariableInstance(caseInstance.getId(), "serializableVar");
        assertThat(variableInstance.getTypeName()).isEqualTo("string");
        assertThat(variableInstance.getValue()).isEqualTo("migrated");

        // The update is logged without the old serializable value
        assertThat(loggingNodes)
                .filteredOn(loggingNode -> LoggingSessionConstants.TYPE_VARIABLE_UPDATE.equals(loggingNode.path("type").asString()))
                .singleElement()
                .satisfies(loggingNode -> {
                    assertThat(loggingNode.path("variableName").asString()).isEqualTo("serializableVar");
                    assertThat(loggingNode.path("variableValue").asString()).isEqualTo("migrated");
                    assertThat(loggingNode.path("oldVariableType").asString()).isEqualTo(SerializableType.TYPE_NAME);
                    assertThat(loggingNode.has("oldVariableValue")).isFalse();
                });
    }

    @Test
    void disabledModeCaseInstanceCanBeTerminatedAndDeleted() {
        CmmnEngine enabledCmmnEngine = buildCmmnEngine(SerializableVariableTypeMode.ENABLED);
        CaseInstance caseInstance = startCaseWithSerializableVariable(enabledCmmnEngine);
        CmmnEngine disabledCmmnEngine = buildCmmnEngine(SerializableVariableTypeMode.DISABLED);

        disabledCmmnEngine.getCmmnRuntimeService().terminateCaseInstance(caseInstance.getId());
        assertThat(disabledCmmnEngine.getCmmnRuntimeService().createCaseInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();

        disabledCmmnEngine.getCmmnHistoryService().deleteHistoricCaseInstance(caseInstance.getId());
        assertThat(disabledCmmnEngine.getCmmnHistoryService().createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
    }

    @Test
    void disabledModeFailsJobWithoutRetries() {
        CmmnEngine disabledCmmnEngine = buildCmmnEngine(SerializableVariableTypeMode.DISABLED);
        disabledCmmnEngine.getCmmnRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/runtime/SerializableVariableTypeModeTest.asyncSerializableVariable.cmmn")
                .deploy();

        CaseInstance caseInstance = disabledCmmnEngine.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("asyncSerializableVariable")
                .start();
        CmmnManagementService managementService = disabledCmmnEngine.getCmmnManagementService();
        Job job = managementService.createJobQuery().caseInstanceId(caseInstance.getId()).singleResult();

        assertThatThrownBy(() -> managementService.executeJob(job.getId()))
                .isExactlyInstanceOf(FlowableUnrecoverableJobException.class)
                .hasMessageContaining("Storing the value of variable 'serializableVar'");

        // Retrying the job would not help, so it is moved to the dead letter jobs right away
        assertThat(managementService.createJobQuery().caseInstanceId(caseInstance.getId()).count()).isZero();
        assertThat(managementService.createDeadLetterJobQuery().caseInstanceId(caseInstance.getId()).singleResult())
                .extracting(Job::getRetries)
                .isEqualTo(0);
    }

    /**
     * Starts a case instance with a serializable variable, using a CMMN engine that has serializable variables enabled.
     */
    protected CaseInstance startCaseWithSerializableVariable(CmmnEngine enabledCmmnEngine) {
        enabledCmmnEngine.getCmmnRepositoryService().createDeployment()
                .addClasspathResource("org/flowable/cmmn/test/one-human-task-model.cmmn")
                .deploy();

        return enabledCmmnEngine.getCmmnRuntimeService().createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variable("serializableVar", new TestSerializableVariable(10))
                .start();
    }

    protected CmmnEngine buildCmmnEngine(SerializableVariableTypeMode mode) {
        return buildCmmnEngine(mode, null);
    }

    protected CmmnEngine buildCmmnEngine(SerializableVariableTypeMode mode, LoggingListener loggingListener) {
        // The first CMMN engine creates the schema (and drops it when it is closed), the other ones are using the same database
        String databaseSchemaUpdate = cmmnEngines.isEmpty() ? CmmnEngineConfiguration.DB_SCHEMA_UPDATE_CREATE_DROP : CmmnEngineConfiguration.DB_SCHEMA_UPDATE_FALSE;
        CmmnEngineConfiguration cmmnEngineConfiguration = new StandaloneInMemCmmnEngineConfiguration();
        cmmnEngineConfiguration.setJdbcUrl(JDBC_URL);
        cmmnEngineConfiguration.setDatabaseSchemaUpdate(databaseSchemaUpdate);
        cmmnEngineConfiguration.setCmmnEngineName("cmmnSerializableVariableTypeModeTest" + cmmnEngines.size());
        cmmnEngineConfiguration.setSerializableVariableTypeMode(mode);
        cmmnEngineConfiguration.setLoggingListener(loggingListener);
        CmmnEngine cmmnEngine = cmmnEngineConfiguration.buildCmmnEngine();
        cmmnEngines.add(cmmnEngine);
        return cmmnEngine;
    }

    public static class SetSerializableVariableDelegate implements PlanItemJavaDelegate {

        @Override
        public void execute(DelegatePlanItemInstance planItemInstance) {
            planItemInstance.setVariable("serializableVar", new TestSerializableVariable(10));
        }
    }

    public static class TestSerializableVariable implements Serializable {

        private static final long serialVersionUID = 1L;

        protected int number;

        public TestSerializableVariable(int number) {
            this.number = number;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof TestSerializableVariable other && number == other.number;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(number);
        }
    }
}
