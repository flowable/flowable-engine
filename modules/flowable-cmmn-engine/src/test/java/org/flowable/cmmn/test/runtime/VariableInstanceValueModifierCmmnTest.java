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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.cmmn.api.history.HistoricCaseInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.DefaultVariableInstanceValueModifier;
import org.flowable.variable.service.impl.VariableInstanceValueModifier;
import org.flowable.variable.service.impl.types.IntegerType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This test checks the basic functionality of the {@link VariableInstanceValueModifier} implementation.
 * It ensures that the modifier is called when setting a variable and that it can be replaced with a custom implementation.
 * Additional tests are available in <code>VariableInstanceValueModifierBpmnTest</code>, as BPMN shares the same implementation of the VariableService.
 *
 * @author Arthur Hupka-Merle
 */
public class VariableInstanceValueModifierCmmnTest extends FlowableCmmnTestCase {

    VariableInstanceValueModifier originalModifier;
    VariableType customType;

    @Before
    public void setUp() {
        originalModifier = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableInstanceValueModifier();
    }

    @After
    public void tearDown() {
        cmmnEngineConfiguration.getVariableServiceConfiguration().setVariableInstanceValueModifier(originalModifier);
        if (customType != null) {
            cmmnEngineConfiguration.getVariableTypes().removeType(customType);
        }
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/one-human-task-model.cmmn" })
    public void testCompleteTaskWithExceptionInPostSetVariable() {
        cmmnEngineConfiguration.getVariableServiceConfiguration()
                .setVariableInstanceValueModifier(new TestOrderIdValidatingValueModifier(cmmnEngineConfiguration.getVariableServiceConfiguration()));

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", 1L);
        variables.put("OtherVariable", "Hello World");

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("oneTaskCase")
                .variables(variables)
                .start();

        assertThatThrownBy(() -> {
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            variables.put("orderId", -1L);
            variables.put("OtherVariable", "Hello World update");
            cmmnTaskService.complete(task.getId(), variables);
        }).isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("Invalid type: value should be larger than zero");
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/one-human-task-model.cmmn" })
    public void testTransientVariables() {
        cmmnEngineConfiguration.getVariableServiceConfiguration()
                .setVariableInstanceValueModifier(new TestOrderIdValidatingValueModifier(cmmnEngineConfiguration.getVariableServiceConfiguration()));

        Map<String, Object> variables = new HashMap<>();
        variables.put("orderId", -1L);
        variables.put("OtherVariable", "Hello World");

        assertThatThrownBy(() -> {
            cmmnRuntimeService.createCaseInstanceBuilder().transientVariables(variables).caseDefinitionKey("oneTaskCase").start();
        }).isInstanceOf(FlowableIllegalArgumentException.class).hasMessage("Invalid type: value should be larger than zero");
    }

    @Test
    @CmmnDeployment(resources = { "org/flowable/cmmn/test/history/testTwoTaskCase.cmmn" })
    public void testWrapVariableWithCustomType() {
        VariableTypes variableTypes = cmmnEngineConfiguration.getVariableServiceConfiguration().getVariableTypes();
        customType = new WrappedIntegerCustomType();
        variableTypes.addTypeBefore(customType, "integer");

        cmmnEngineConfiguration.getVariableServiceConfiguration()
                .setVariableInstanceValueModifier(new WrappedIntegerValueModifier(cmmnEngineConfiguration.getVariableServiceConfiguration()));

        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .variable("simpleInteger", 1000)
                .variable("wrappedInteger", 1001)
                .caseDefinitionKey("myCase")
                .start();

        VariableInstance simpleInteger = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "simpleInteger");
        assertThat(simpleInteger.getTypeName()).isEqualTo("integer");
        assertThat(simpleInteger.getValue()).isEqualTo(1000);

        VariableInstance wrappedInteger = cmmnRuntimeService.getVariableInstance(caseInstance.getId(), "wrappedInteger");
        assertThat(wrappedInteger.getTypeName()).isEqualTo("wrappedIntegerType");
        assertThat(wrappedInteger.getValue()).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
            assertThat(wrappedIntegerValue.value).isEqualTo(1001);
            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
        });
        Object wrappedIntegerCaseVariable = caseInstance.getCaseVariables().get("wrappedInteger");
        assertThat(wrappedIntegerCaseVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
            assertThat(wrappedIntegerValue.value).isEqualTo(1001);
            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
        });

        caseInstance = cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables()
                .singleResult();

        wrappedIntegerCaseVariable = caseInstance.getCaseVariables().get("wrappedInteger");
        assertThat(wrappedIntegerCaseVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
            assertThat(wrappedIntegerValue.value).isEqualTo(1001);
            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
        });

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .variableName("wrappedInteger")
                    .singleResult();
            assertThat(historicVariableInstance).isNotNull();
            assertThat(historicVariableInstance.getValue())
                    .isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                        assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                        assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
                    });
            assertThat(historicVariableInstance.getVariableTypeName()).isEqualTo("wrappedIntegerType");
            assertThat(historicVariableInstance.getMetaInfo()).isEqualTo("1001meta");

            HistoricCaseInstance historicCaseInstance = cmmnHistoryService.createHistoricCaseInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .includeCaseVariables()
                    .singleResult();

            wrappedIntegerCaseVariable = historicCaseInstance.getCaseVariables().get("wrappedInteger");
            assertThat(wrappedIntegerCaseVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
            });

            HistoricTaskInstance historicTask = cmmnHistoryService.createHistoricTaskInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .includeCaseVariables()
                    .singleResult();

            wrappedIntegerCaseVariable = historicTask.getCaseVariables().get("wrappedInteger");
            assertThat(wrappedIntegerCaseVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                assertThat(wrappedIntegerValue.value).isEqualTo(1001);
                assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
            });
        }

        Task task = cmmnTaskService.createTaskQuery()
                .caseInstanceId(caseInstance.getId())
                .includeCaseVariables()
                .singleResult();

        wrappedIntegerCaseVariable = task.getCaseVariables().get("wrappedInteger");
        assertThat(wrappedIntegerCaseVariable).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
            assertThat(wrappedIntegerValue.value).isEqualTo(1001);
            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1001meta");
        });

        cmmnTaskService.complete(task.getId(), Collections.singletonMap("wrappedInteger", 1002));

        VariableInstance wrappedIntegerTaskUpdate = cmmnRuntimeService.getVariableInstance(caseInstance.getId(),
                "wrappedInteger");
        assertThat(wrappedIntegerTaskUpdate.getValue()).isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
            assertThat(wrappedIntegerValue.value).isEqualTo(1002);
            assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1002meta");
        });

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricVariableInstance historicVariableInstance = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .variableName("wrappedInteger")
                    .singleResult();
            assertThat(historicVariableInstance).isNotNull();
            assertThat(historicVariableInstance.getValue())
                    .isInstanceOfSatisfying(WrappedIntegerValue.class, wrappedIntegerValue -> {
                        assertThat(wrappedIntegerValue.value).isEqualTo(1002);
                        assertThat(wrappedIntegerValue.metaInfo).isEqualTo("1002meta");
                    });
            assertThat(historicVariableInstance.getVariableTypeName()).isEqualTo("wrappedIntegerType");
            assertThat(historicVariableInstance.getMetaInfo()).isEqualTo("1002meta");
        }
    }


    public static class WrappedIntegerCustomType extends IntegerType {

        public static final String TYPE_NAME = "wrappedIntegerType";

        @Override
        public String getTypeName() {
            return TYPE_NAME;
        }

        @Override
        public void setValue(Object value, ValueFields valueFields) {
            if (value instanceof WrappedIntegerValue) {
                super.setValue(((WrappedIntegerValue) value).value, valueFields);
            } else {
                super.setValue(value, valueFields);
            }
        }

        @Override
        public Object getValue(ValueFields valueFields) {
            if (valueFields instanceof VariableInstance) {
                return new WrappedIntegerValue(valueFields.getLongValue(), ((VariableInstance) valueFields).getMetaInfo());
            } else if (valueFields instanceof HistoricVariableInstance) {
                return new WrappedIntegerValue(valueFields.getLongValue(), ((HistoricVariableInstance) valueFields).getMetaInfo());
            }
            return super.getValue(valueFields);
        }

        @Override
        public boolean isAbleToStore(Object value) {
            if (value == null) {
                return true;
            }
            return value instanceof WrappedIntegerValue;
        }
    }

    static class WrappedIntegerValue {

        public String metaInfo;
        public Integer value;

        public WrappedIntegerValue(Number value, String metaInfo) {
            this.metaInfo = metaInfo;
            this.value = value.intValue();
        }
    }

    static class TestOrderIdValidatingValueModifier extends DefaultVariableInstanceValueModifier {

        public TestOrderIdValidatingValueModifier(VariableServiceConfiguration serviceConfiguration) {
            super(serviceConfiguration);
        }

        @Override
        public void setVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
            validateValue(variableInstance, value);
            super.setVariableValue(variableInstance, value, tenantId);
        }

        @Override
        public void updateVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
            validateValue(variableInstance, value);
            super.updateVariableValue(variableInstance, value, tenantId);
        }

        protected void validateValue(VariableInstance variableInstance, Object value) {
            if (variableInstance.getName().equals("orderId")) {
                if (((Number) value).longValue() < 0) {
                    throw new FlowableIllegalArgumentException("Invalid type: value should be larger than zero");
                }
            }
        }
    }

    static class WrappedIntegerValueModifier extends DefaultVariableInstanceValueModifier {

        WrappedIntegerValueModifier(VariableServiceConfiguration serviceConfiguration) {
            super(serviceConfiguration);
        }


        @Override
        public void setVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
            Pair<Object, String> valueAndMeta = determineValueAndMeta(value);
            super.setVariableValue(variableInstance, valueAndMeta.getLeft(), tenantId);
            variableInstance.setMetaInfo(valueAndMeta.getRight());
        }

        @Override
        public void updateVariableValue(VariableInstance variableInstance, Object value, String tenantId) {
            Pair<Object, String> valueAndMeta = determineValueAndMeta(value);
            super.updateVariableValue(variableInstance, valueAndMeta.getLeft(), tenantId);
            variableInstance.setMetaInfo(valueAndMeta.getRight());
        }

        Pair<Object, String> determineValueAndMeta(Object value) {
            if (value instanceof Integer && ((Integer) value) > 1000) {
                String metaInfo = value + "meta";
                return Pair.of(new WrappedIntegerValue((Number) value, metaInfo), metaInfo);
            }
            return Pair.of(value, null);
        }
    }
}
