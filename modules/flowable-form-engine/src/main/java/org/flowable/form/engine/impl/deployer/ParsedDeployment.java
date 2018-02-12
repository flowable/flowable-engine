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
package org.flowable.form.engine.impl.deployer;

import java.util.List;
import java.util.Map;

import org.flowable.form.engine.impl.parser.FormDefinitionParse;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.flowable.form.engine.impl.persistence.entity.FormResourceEntity;
import org.flowable.form.model.SimpleFormModel;

/**
 * An intermediate representation of a DeploymentEntity which keeps track of all of the entity's and resources.
 * 
 * The FormDefinitionEntities are expected to be "not fully set-up" - they may be inconsistent with the DeploymentEntity and/or the persisted versions, and if the deployment is new, they will not yet
 * be persisted.
 */
public class ParsedDeployment {

    protected FormDeploymentEntity deploymentEntity;

    protected List<FormDefinitionEntity> formDefinitions;
    protected Map<FormDefinitionEntity, FormDefinitionParse> mapFormDefinitionsToParses;
    protected Map<FormDefinitionEntity, FormResourceEntity> mapFormDefinitionsToResources;

    public ParsedDeployment(
            FormDeploymentEntity entity, List<FormDefinitionEntity> formDefinitions,
            Map<FormDefinitionEntity, FormDefinitionParse> mapFormDefinitionsToParses,
            Map<FormDefinitionEntity, FormResourceEntity> mapFormDefinitionsToResources) {

        this.deploymentEntity = entity;
        this.formDefinitions = formDefinitions;
        this.mapFormDefinitionsToParses = mapFormDefinitionsToParses;
        this.mapFormDefinitionsToResources = mapFormDefinitionsToResources;
    }

    public FormDeploymentEntity getDeployment() {
        return deploymentEntity;
    }

    public List<FormDefinitionEntity> getAllFormDefinitions() {
        return formDefinitions;
    }

    public FormResourceEntity getResourceForFormDefinition(FormDefinitionEntity formDefinition) {
        return mapFormDefinitionsToResources.get(formDefinition);
    }

    public FormDefinitionParse getFormDefinitionParseForFormDefinition(FormDefinitionEntity formDefinition) {
        return mapFormDefinitionsToParses.get(formDefinition);
    }

    public SimpleFormModel getFormModelForFormDefinition(FormDefinitionEntity formDefinition) {
        FormDefinitionParse parse = getFormDefinitionParseForFormDefinition(formDefinition);

        return (parse == null ? null : parse.getFormModel());
    }
}
