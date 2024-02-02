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
package org.flowable.dmn.engine.impl.deployer;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.repository.EngineResource;
import org.flowable.dmn.engine.impl.parser.DmnParse;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DmnDefinition;

/**
 * An intermediate representation of a DeploymentEntity which keeps track of all of the entity's DefinitionEntities and resources and processes associated with each
 * DefinitionEntity - all produced by parsing the deployment.
 * 
 * The DefinitionEntities are expected to be "not fully set-up" - they may be inconsistent with the DeploymentEntity and/or the persisted versions, and if the deployment is new, they will not yet
 * be persisted.
 */
public class ParsedDeployment {

    protected DmnDeploymentEntity deploymentEntity;

    protected List<DecisionEntity> decisions;
    protected Map<DecisionEntity, DmnParse> mapDecisionsToParses;
    protected Map<DecisionEntity, EngineResource> mapDecisionsToResources;

    public ParsedDeployment(
            DmnDeploymentEntity entity, List<DecisionEntity> decisions,
            Map<DecisionEntity, DmnParse> mapDecisionsToParses,
            Map<DecisionEntity, EngineResource> mapDecisionsToResources) {

        this.deploymentEntity = entity;
        this.decisions = decisions;
        this.mapDecisionsToParses = mapDecisionsToParses;
        this.mapDecisionsToResources = mapDecisionsToResources;
    }

    public DmnDeploymentEntity getDeployment() {
        return deploymentEntity;
    }

    public List<DecisionEntity> getAllDecisions() {
        return decisions;
    }

    public EngineResource getResourceForDecision(DecisionEntity decision) {
        return mapDecisionsToResources.get(decision);
    }

    public DmnParse getDmnParseForDecision(DecisionEntity decision) {
        return mapDecisionsToParses.get(decision);
    }

    public DmnDefinition getDmnDefinitionForDecision(DecisionEntity decision) {
        DmnParse parse = getDmnParseForDecision(decision);

        return (parse == null ? null : parse.getDmnDefinition());
    }

    public DecisionService getDecisionServiceForDecisionEntity(DecisionEntity decisionEntity) {
        DmnDefinition dmnDefinition = getDmnDefinitionForDecision(decisionEntity);

        return (dmnDefinition == null ? null : dmnDefinition.getDecisionServiceById(decisionEntity.getKey()));
    }

    public Decision getDecisionForDecisionEntity(DecisionEntity decisionEntity) {
        DmnDefinition dmnDefinition = getDmnDefinitionForDecision(decisionEntity);

        return (dmnDefinition == null ? null : dmnDefinition.getDecisionById(decisionEntity.getKey()));
    }
}
