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
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntity;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.engine.common.api.FlowableException;

/**
 * @author Joram Barrez
 */
public class CmmnParseResult {

    protected CmmnDeploymentEntity deploymentEntity;
    protected List<CaseDefinitionEntity> definitions = new ArrayList<>();
    protected Map<CaseDefinitionEntity, CmmnModel> mapDefinitionsToCmmnModel = new HashMap<>();
    protected Map<CaseDefinitionEntity, CmmnResourceEntity> mapDefinitionsToResources = new HashMap<>();
    
    public CmmnParseResult() {
        
    }

    public CmmnParseResult(CmmnDeploymentEntity entity) {
        this.deploymentEntity = entity;
    }

    public CmmnDeploymentEntity getDeployment() {
        return deploymentEntity;
    }
    
    public void addCaseDefinition(CaseDefinitionEntity caseDefinitionEntity) {
        definitions.add(caseDefinitionEntity);
    }

    public List<CaseDefinitionEntity> getAllCaseDefinitions() {
        return definitions;
    }
    
    public void addCaseDefinition(CaseDefinitionEntity caseDefinitionEntity, CmmnResourceEntity resourceEntity, CmmnModel cmmnModel) {
        definitions.add(caseDefinitionEntity);
        mapDefinitionsToResources.put(caseDefinitionEntity, resourceEntity);
        mapDefinitionsToCmmnModel.put(caseDefinitionEntity, cmmnModel);
    }
    
    public CmmnResourceEntity getResourceForCaseDefinition(CaseDefinitionEntity caseDefinition) {
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
        if (deploymentEntity == null) {
            throw new FlowableException("Cannot merge from a parse result without a deployment entity");
        }
        if (cmmnParseResult.getDeployment() != null && !deploymentEntity.equals(cmmnParseResult.getDeployment())) {
            throw new FlowableException("Cannot merge parse results with different deployment entities");
        }
        for (CaseDefinitionEntity caseDefinitionEntity : cmmnParseResult.getAllCaseDefinitions()) {
            addCaseDefinition(caseDefinitionEntity,
                    cmmnParseResult.getResourceForCaseDefinition(caseDefinitionEntity),
                    cmmnParseResult.getCmmnModelForCaseDefinition(caseDefinitionEntity));
        }
    }

}
