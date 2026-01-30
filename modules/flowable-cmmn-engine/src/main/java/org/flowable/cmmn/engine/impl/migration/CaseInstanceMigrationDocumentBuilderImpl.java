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

package org.flowable.cmmn.engine.impl.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.migration.ActivatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocument;
import org.flowable.cmmn.api.migration.CaseInstanceMigrationDocumentBuilder;
import org.flowable.cmmn.api.migration.ChangePlanItemDefinitionWithNewTargetIdsMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdMapping;
import org.flowable.cmmn.api.migration.ChangePlanItemIdWithDefinitionIdMapping;
import org.flowable.cmmn.api.migration.MoveToAvailablePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.RemoveWaitingForRepetitionPlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.TerminatePlanItemDefinitionMapping;
import org.flowable.cmmn.api.migration.WaitingForRepetitionPlanItemDefinitionMapping;

/**
 * @author Valentin Zickner
 */
public class CaseInstanceMigrationDocumentBuilderImpl implements CaseInstanceMigrationDocumentBuilder {

    protected String migrateToCaseDefinitionId;
    protected String migrateToCaseDefinitionKey;
    protected Integer migrateToCaseDefinitionVersion;
    protected String migrateToCaseDefinitionTenantId;
    protected Boolean enableAutomaticPlanItemInstanceCreation;
    protected List<ActivatePlanItemDefinitionMapping> activatePlanItemDefinitionMappings = new ArrayList<>();
    protected List<TerminatePlanItemDefinitionMapping> terminatePlanItemDefinitionMappings = new ArrayList<>();
    protected List<MoveToAvailablePlanItemDefinitionMapping> moveToAvailablePlanItemDefinitionMappings = new ArrayList<>();
    protected List<WaitingForRepetitionPlanItemDefinitionMapping> waitingForRepetitionPlanItemDefinitionMappings = new ArrayList<>();
    protected List<RemoveWaitingForRepetitionPlanItemDefinitionMapping> removeWaitingForRepetitionPlanItemDefinitionMappings = new ArrayList<>();
    protected List<ChangePlanItemIdMapping> changePlanItemIdMappings = new ArrayList<>();
    protected List<ChangePlanItemIdWithDefinitionIdMapping> changePlanItemIdWithDefinitionIdMappings = new ArrayList<>();
    protected List<ChangePlanItemDefinitionWithNewTargetIdsMapping> changePlanItemDefinitionWithNewTargetIdsMappings = new ArrayList<>();
    protected String preUpgradeExpression;
    protected String postUpgradeExpression;
    protected Map<String, Object> caseInstanceVariables = new HashMap<>();

    @Override
    public CaseInstanceMigrationDocumentBuilder setCaseDefinitionToMigrateTo(String caseDefinitionId) {
        this.migrateToCaseDefinitionId = caseDefinitionId;
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder setCaseDefinitionToMigrateTo(String caseDefinitionKey, Integer caseDefinitionVersion) {
        this.migrateToCaseDefinitionKey = caseDefinitionKey;
        this.migrateToCaseDefinitionVersion = caseDefinitionVersion;
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder setTenantId(String caseDefinitionTenantId) {
        this.migrateToCaseDefinitionTenantId = caseDefinitionTenantId;
        return this;
    }
    

    @Override
    public CaseInstanceMigrationDocumentBuilder setEnableAutomaticPlanItemInstanceCreation(Boolean automaticValue) {
        this.enableAutomaticPlanItemInstanceCreation = automaticValue;
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addActivatePlanItemDefinitionMappings(List<ActivatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.activatePlanItemDefinitionMappings.addAll(planItemDefinitionMappings);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addActivatePlanItemDefinitionMapping(ActivatePlanItemDefinitionMapping planItemDefinitionMapping) {
        this.activatePlanItemDefinitionMappings.add(planItemDefinitionMapping);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addTerminatePlanItemDefinitionMappings(List<TerminatePlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.terminatePlanItemDefinitionMappings.addAll(planItemDefinitionMappings);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addTerminatePlanItemDefinitionMapping(TerminatePlanItemDefinitionMapping planItemDefinitionMapping) {
        this.terminatePlanItemDefinitionMappings.add(planItemDefinitionMapping);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addMoveToAvailablePlanItemDefinitionMappings(List<MoveToAvailablePlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.moveToAvailablePlanItemDefinitionMappings.addAll(planItemDefinitionMappings);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addMoveToAvailablePlanItemDefinitionMapping(MoveToAvailablePlanItemDefinitionMapping planItemDefinitionMapping) {
        this.moveToAvailablePlanItemDefinitionMappings.add(planItemDefinitionMapping);
        return this;
    }
    
    @Override
    public CaseInstanceMigrationDocumentBuilder addWaitingForRepetitionPlanItemDefinitionMappings(List<WaitingForRepetitionPlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.waitingForRepetitionPlanItemDefinitionMappings.addAll(planItemDefinitionMappings);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addWaitingForRepetitionPlanItemDefinitionMapping(WaitingForRepetitionPlanItemDefinitionMapping planItemDefinitionMapping) {
        this.waitingForRepetitionPlanItemDefinitionMappings.add(planItemDefinitionMapping);
        return this;
    }
    
    @Override
    public CaseInstanceMigrationDocumentBuilder addRemoveWaitingForRepetitionPlanItemDefinitionMappings(List<RemoveWaitingForRepetitionPlanItemDefinitionMapping> planItemDefinitionMappings) {
        this.removeWaitingForRepetitionPlanItemDefinitionMappings.addAll(planItemDefinitionMappings);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addRemoveWaitingForRepetitionPlanItemDefinitionMapping(RemoveWaitingForRepetitionPlanItemDefinitionMapping planItemDefinitionMapping) {
        this.removeWaitingForRepetitionPlanItemDefinitionMappings.add(planItemDefinitionMapping);
        return this;
    }
    
    @Override
    public CaseInstanceMigrationDocumentBuilder addChangePlanItemIdMapping(ChangePlanItemIdMapping mapping) {
        this.changePlanItemIdMappings.add(mapping);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addChangePlanItemIdMappings(List<ChangePlanItemIdMapping> mappings) {
        this.changePlanItemIdMappings.addAll(mappings);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addChangePlanItemIdWithDefinitionIdMapping(ChangePlanItemIdWithDefinitionIdMapping mapping) {
        this.changePlanItemIdWithDefinitionIdMappings.add(mapping);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addChangePlanItemIdWithDefinitionIdMappings(List<ChangePlanItemIdWithDefinitionIdMapping> mappings) {
        this.changePlanItemIdWithDefinitionIdMappings.addAll(mappings);
        return this;
    }
    
    @Override
    public CaseInstanceMigrationDocumentBuilder addChangePlanItemDefinitionWithNewTargetIdsMapping(ChangePlanItemDefinitionWithNewTargetIdsMapping mapping) {
        this.changePlanItemDefinitionWithNewTargetIdsMappings.add(mapping);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addChangePlanItemDefinitionWithNewTargetIdsMappings(List<ChangePlanItemDefinitionWithNewTargetIdsMapping> mappings) {
        this.changePlanItemDefinitionWithNewTargetIdsMappings.addAll(mappings);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addCaseInstanceVariable(String variableName, Object variableValue) {
        this.caseInstanceVariables.put(variableName, variableValue);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder addCaseInstanceVariables(Map<String, Object> caseInstanceVariables) {
        this.caseInstanceVariables.putAll(caseInstanceVariables);
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder preUpgradeExpression(String preUpgradeExpression) {
        this.preUpgradeExpression = preUpgradeExpression;
        return this;
    }

    @Override
    public CaseInstanceMigrationDocumentBuilder postUpgradeExpression(String postUpgradeExpression) {
        this.postUpgradeExpression = postUpgradeExpression;
        return this;
    }

    @Override
    public CaseInstanceMigrationDocument build() {
        CaseInstanceMigrationDocumentImpl caseInstanceMigrationDocument = new CaseInstanceMigrationDocumentImpl();
        caseInstanceMigrationDocument.setMigrateToCaseDefinitionId(this.migrateToCaseDefinitionId);
        caseInstanceMigrationDocument.setMigrateToCaseDefinition(this.migrateToCaseDefinitionKey, this.migrateToCaseDefinitionVersion, this.migrateToCaseDefinitionTenantId);
        caseInstanceMigrationDocument.setEnableAutomaticPlanItemInstanceCreation(this.enableAutomaticPlanItemInstanceCreation);
        caseInstanceMigrationDocument.setActivatePlanItemDefinitionMappings(this.activatePlanItemDefinitionMappings);
        caseInstanceMigrationDocument.setTerminatePlanItemDefinitionMappings(this.terminatePlanItemDefinitionMappings);
        caseInstanceMigrationDocument.setMoveToAvailablePlanItemDefinitionMappings(this.moveToAvailablePlanItemDefinitionMappings);
        caseInstanceMigrationDocument.setWaitingForRepetitionPlanItemDefinitionMappings(this.waitingForRepetitionPlanItemDefinitionMappings);
        caseInstanceMigrationDocument.setRemoveWaitingForRepetitionPlanItemDefinitionMappings(this.removeWaitingForRepetitionPlanItemDefinitionMappings);
        caseInstanceMigrationDocument.setChangePlanItemIdMappings(this.changePlanItemIdMappings);
        caseInstanceMigrationDocument.setChangePlanItemIdWithDefinitionIdMappings(this.changePlanItemIdWithDefinitionIdMappings);
        caseInstanceMigrationDocument.setChangePlanItemDefinitionWithNewTargetIdsMappings(this.changePlanItemDefinitionWithNewTargetIdsMappings);
        caseInstanceMigrationDocument.setCaseInstanceVariables(this.caseInstanceVariables);
        caseInstanceMigrationDocument.setPreUpgradeExpression(this.preUpgradeExpression);
        caseInstanceMigrationDocument.setPostUpgradeExpression(this.postUpgradeExpression);
        return caseInstanceMigrationDocument;
    }
}
