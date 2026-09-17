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
package org.flowable.engine.test.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.service.InternalVariableInstanceQuery;
import org.flowable.variable.service.VariableService;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests that the variable queries split id collections which exceed {@link AbstractDataManager#MAX_ENTRIES_IN_CLAUSE}
 * into multiple in() clauses, since not every database supports more entries in a single in() clause.
 *
 * @author Filip Hrisafov
 */
class VariableInstanceQueryLargeIdCollectionTest extends PluggableFlowableTestCase {

    protected final List<String> variableIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        managementService.executeCommand(commandContext -> {
            VariableService variableService = processEngineConfiguration.getVariableServiceConfiguration().getVariableService();
            for (String variableId : variableIds) {
                VariableInstanceEntity variable = variableService.createInternalVariableInstanceQuery().id(variableId).singleResult();
                if (variable != null) {
                    variableService.deleteVariableInstance(variable);
                }
            }
            return null;
        });
        variableIds.clear();
    }

    @Test
    void internalQueryWithLargeTaskIdCollection() {
        createVariable("taskVar", variable -> variable.setTaskId("task-1"));
        createVariable("otherTaskVar", variable -> variable.setTaskId("task-2"));
        createVariable("ignoredTaskVar", variable -> variable.setTaskId("task-3"));

        assertThat(findVariables(query -> query.taskIds(idsWith("task-1", "task-2"))))
                .extracting(VariableInstance::getName)
                .containsExactlyInAnyOrder("taskVar", "otherTaskVar");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void internalQueryWithLargeExecutionIdCollection() {
        // Variables are linked to an execution with a foreign key, so real process instances are needed here
        ProcessInstance firstInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Map.of("executionVar", "first"));
        ProcessInstance secondInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Map.of("otherExecutionVar", "second"));
        runtimeService.startProcessInstanceByKey("oneTaskProcess", Map.of("ignoredExecutionVar", "third"));

        assertThat(findVariables(query -> query.executionIds(idsWith(firstInstance.getId(), secondInstance.getId()))))
                .extracting(VariableInstance::getName)
                .containsExactlyInAnyOrder("executionVar", "otherExecutionVar");
    }

    @Test
    void internalQueryWithLargeScopeIdCollection() {
        createVariable("scopeVar", variable -> {
            variable.setScopeId("scope-1");
            variable.setScopeType(ScopeTypes.CMMN);
        });
        createVariable("otherScopeVar", variable -> {
            variable.setScopeId("scope-2");
            variable.setScopeType(ScopeTypes.CMMN);
        });
        createVariable("ignoredScopeVar", variable -> {
            variable.setScopeId("scope-3");
            variable.setScopeType(ScopeTypes.CMMN);
        });

        assertThat(findVariables(query -> query.scopeIds(idsWith("scope-1", "scope-2")).scopeType(ScopeTypes.CMMN)))
                .extracting(VariableInstance::getName)
                .containsExactlyInAnyOrder("scopeVar", "otherScopeVar");
    }

    @Test
    void internalQueryWithLargeSubScopeIdCollection() {
        createVariable("subScopeVar", variable -> {
            variable.setSubScopeId("subScope-1");
            variable.setScopeType(ScopeTypes.CMMN);
        });
        createVariable("otherSubScopeVar", variable -> {
            variable.setSubScopeId("subScope-2");
            variable.setScopeType(ScopeTypes.CMMN);
        });
        createVariable("ignoredSubScopeVar", variable -> {
            variable.setSubScopeId("subScope-3");
            variable.setScopeType(ScopeTypes.CMMN);
        });

        assertThat(findVariables(query -> query.subScopeIds(idsWith("subScope-1", "subScope-2")).scopeType(ScopeTypes.CMMN)))
                .extracting(VariableInstance::getName)
                .containsExactlyInAnyOrder("subScopeVar", "otherSubScopeVar");
    }

    @Test
    void variableInstanceQueryWithLargeTaskIdCollection() {
        createVariable("taskVar", variable -> variable.setTaskId("task-1"));
        createVariable("otherTaskVar", variable -> variable.setTaskId("task-2"));
        createVariable("ignoredTaskVar", variable -> variable.setTaskId("task-3"));

        assertThat(runtimeService.createVariableInstanceQuery().taskIds(idsWith("task-1", "task-2")).list())
                .extracting(VariableInstance::getName)
                .containsExactlyInAnyOrder("taskVar", "otherTaskVar");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void variableInstanceQueryWithLargeExecutionIdCollection() {
        ProcessInstance firstInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Map.of("executionVar", "first"));
        ProcessInstance secondInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Map.of("otherExecutionVar", "second"));
        runtimeService.startProcessInstanceByKey("oneTaskProcess", Map.of("ignoredExecutionVar", "third"));

        assertThat(runtimeService.createVariableInstanceQuery().executionIds(idsWith(firstInstance.getId(), secondInstance.getId())).list())
                .extracting(VariableInstance::getName)
                .containsExactlyInAnyOrder("executionVar", "otherExecutionVar");
    }

    @Test
    @Deployment(resources = "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
    void historicVariableInstanceQueryWithLargeExecutionIdCollection() {
        ProcessInstance firstInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Map.of("executionVar", "first"));
        ProcessInstance secondInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", Map.of("otherExecutionVar", "second"));
        runtimeService.startProcessInstanceByKey("oneTaskProcess", Map.of("ignoredExecutionVar", "third"));

        assertThat(historyService.createHistoricVariableInstanceQuery().executionIds(idsWith(firstInstance.getId(), secondInstance.getId())).list())
                .extracting(HistoricVariableInstance::getVariableName)
                .containsExactlyInAnyOrder("executionVar", "otherExecutionVar");
    }

    protected List<VariableInstanceEntity> findVariables(Consumer<InternalVariableInstanceQuery> queryCustomizer) {
        return managementService.executeCommand(commandContext -> {
            InternalVariableInstanceQuery query = processEngineConfiguration.getVariableServiceConfiguration()
                    .getVariableService()
                    .createInternalVariableInstanceQuery();
            queryCustomizer.accept(query);
            return query.list();
        });
    }

    protected void createVariable(String name, Consumer<VariableInstanceEntity> variableCustomizer) {
        String variableId = managementService.executeCommand(commandContext -> {
            VariableService variableService = processEngineConfiguration.getVariableServiceConfiguration().getVariableService();
            VariableInstanceEntity variable = variableService.createVariableInstance(name);
            variableCustomizer.accept(variable);
            variableService.insertVariableInstanceWithValue(variable, name + "-value", null);
            return variable.getId();
        });
        variableIds.add(variableId);
    }

    /**
     * Returns a set which contains the given ids, padded with dummy ids so the total exceeds the maximum number of entries in a single in() clause.
     */
    protected Set<String> idsWith(String... ids) {
        Set<String> allIds = new LinkedHashSet<>(List.of(ids));
        allIds.addAll(generateIds("dummy", AbstractDataManager.MAX_ENTRIES_IN_CLAUSE + 500));
        return allIds;
    }

    protected Collection<String> generateIds(String prefix, int amount) {
        List<String> ids = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            ids.add(prefix + "-generated-" + i);
        }
        return ids;
    }
}
