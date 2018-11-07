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
package org.flowable.cmmn.engine.impl.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.repository.EngineDeployment;
import org.flowable.common.engine.api.repository.EngineResource;

/**
 * @author Joram Barrez
 */
public class CmmnParseResult {

    protected EngineDeployment deployment;
    protected List<CaseDefinitionEntity> definitions = new ArrayList<>();
    protected Map<CaseDefinitionEntity, CmmnModel> mapDefinitionsToCmmnModel = new HashMap<>();
    protected Map<CaseDefinitionEntity, EngineResource> mapDefinitionsToResources = new HashMap<>();
    
    public CmmnParseResult() {
        
    }

    public CmmnParseResult(EngineDeployment deployment) {
        this.deployment = deployment;
    }

    public EngineDeployment getDeployment() {
        return deployment;
    }
    
    public void addCaseDefinition(CaseDefinitionEntity caseDefinitionEntity) {
        definitions.add(caseDefinitionEntity);
    }

    public List<CaseDefinitionEntity> getAllCaseDefinitions() {
        return definitions;
    }
    
    public void addCaseDefinition(CaseDefinitionEntity caseDefinitionEntity, EngineResource resourceEntity, CmmnModel cmmnModel) {
        definitions.add(caseDefinitionEntity);
        mapDefinitionsToResources.put(caseDefinitionEntity, resourceEntity);
        mapDefinitionsToCmmnModel.put(caseDefinitionEntity, cmmnModel);
    }
    
    public EngineResource getResourceForCaseDefinition(CaseDefinitionEntity caseDefinition) {
        return mapDefinitionsToResources.get(caseDefinition);
    }

    public CmmnModel getCmmnModelForCaseDefinition(CaseDefinitionEntity caseDefinition) {
        return mapDefinitionsToCmmnModel.get(caseDefinition);
    }

    public Case getCmmnCaseForCaseDefinition(CaseDefinitionEntity caseDefinition) {
        CmmnModel model = getCmmnModelForCaseDefinition(caseDefinition);
        return (model == null ? null : model.getCaseById(caseDefinition.getKey()));
    }
    
    public void merge(CmmnParseResult cmmnParseResult) {
        if (deployment == null) {
            throw new FlowableException("Cannot merge from a parse result without a deployment entity");
        }
        if (cmmnParseResult.getDeployment() != null && !deployment.equals(cmmnParseResult.getDeployment())) {
            throw new FlowableException("Cannot merge parse results with different deployment entities");
        }
        for (CaseDefinitionEntity caseDefinitionEntity : cmmnParseResult.getAllCaseDefinitions()) {
            addCaseDefinition(caseDefinitionEntity,
                    cmmnParseResult.getResourceForCaseDefinition(caseDefinitionEntity),
                    cmmnParseResult.getCmmnModelForCaseDefinition(caseDefinitionEntity));
        }
    }

}
