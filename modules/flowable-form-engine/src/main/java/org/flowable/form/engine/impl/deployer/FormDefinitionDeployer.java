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

import java.util.LinkedHashMap;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.form.engine.impl.persistence.deploy.Deployer;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDefinitionEntityManager;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class FormDefinitionDeployer implements Deployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FormDefinitionDeployer.class);

    protected IdGenerator idGenerator;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected FormDefinitionDeploymentHelper formDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;

    @Override
    public void deploy(FormDeploymentEntity deployment) {
        LOGGER.debug("Processing deployment {}", deployment.getName());

        // The ParsedDeployment represents the deployment, the forms, and the form
        // resource, parse, and model associated with each form.
        ParsedDeployment parsedDeployment = parsedDeploymentBuilderFactory.getBuilderForDeployment(deployment).build();

        formDeploymentHelper.verifyFormsDoNotShareKeys(parsedDeployment.getAllFormDefinitions());

        formDeploymentHelper.copyDeploymentValuesToForms(parsedDeployment.getDeployment(), parsedDeployment.getAllFormDefinitions());
        formDeploymentHelper.setResourceNamesOnFormDefinitions(parsedDeployment);

        if (deployment.isNew()) {
            Map<FormDefinitionEntity, FormDefinitionEntity> mapOfNewFormToPreviousVersion = getPreviousVersionsOfFormDefinitions(parsedDeployment);
            setFormDefinitionVersionsAndIds(parsedDeployment, mapOfNewFormToPreviousVersion);
            persistFormDefinitions(parsedDeployment);
        } else {
            makeFormDefinitionsConsistentWithPersistedVersions(parsedDeployment);
        }

        cachingAndArtifactsManager.updateCachingAndArtifacts(parsedDeployment);
    }

    /**
     * Constructs a map from new FormEntities to the previous version by key and tenant. If no previous version exists, no map entry is created.
     */
    protected Map<FormDefinitionEntity, FormDefinitionEntity> getPreviousVersionsOfFormDefinitions(ParsedDeployment parsedDeployment) {

        Map<FormDefinitionEntity, FormDefinitionEntity> result = new LinkedHashMap<>();

        for (FormDefinitionEntity newDefinition : parsedDeployment.getAllFormDefinitions()) {
            FormDefinitionEntity existingFormDefinition = formDeploymentHelper.getMostRecentVersionOfForm(newDefinition);

            if (existingFormDefinition != null) {
                result.put(newDefinition, existingFormDefinition);
            }
        }

        return result;
    }

    /**
     * Sets the version on each form entity, and the identifier. If the map contains an older version for a form, then the version is set to that older entity's version plus one; otherwise it is set
     * to 1.
     */
    protected void setFormDefinitionVersionsAndIds(ParsedDeployment parsedDeployment, Map<FormDefinitionEntity, FormDefinitionEntity> mapNewToOldForms) {

        for (FormDefinitionEntity formDefinition : parsedDeployment.getAllFormDefinitions()) {
            int version = 1;

            FormDefinitionEntity latest = mapNewToOldForms.get(formDefinition);
            if (latest != null) {
                version = latest.getVersion() + 1;
            }

            formDefinition.setVersion(version);
            formDefinition.setId(idGenerator.getNextId());
        }
    }

    /**
     * Saves each decision table. It is assumed that the deployment is new, the definitions have never been saved before, and that they have all their values properly set up.
     */
    protected void persistFormDefinitions(ParsedDeployment parsedDeployment) {
        FormDefinitionEntityManager formDefinitionEntityManager = CommandContextUtil.getFormDefinitionEntityManager();

        for (FormDefinitionEntity formDefinition : parsedDeployment.getAllFormDefinitions()) {
            formDefinitionEntityManager.insert(formDefinition);
        }
    }

    /**
     * Loads the persisted version of each form and set values on the in-memory version to be consistent.
     */
    protected void makeFormDefinitionsConsistentWithPersistedVersions(ParsedDeployment parsedDeployment) {
        for (FormDefinitionEntity formDefinition : parsedDeployment.getAllFormDefinitions()) {
            FormDefinitionEntity persistedFormDefinition = formDeploymentHelper.getPersistedInstanceOfFormDefinition(formDefinition);

            if (persistedFormDefinition != null) {
                formDefinition.setId(persistedFormDefinition.getId());
                formDefinition.setVersion(persistedFormDefinition.getVersion());
            }
        }
    }

    public IdGenerator getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

    public ParsedDeploymentBuilderFactory getExParsedDeploymentBuilderFactory() {
        return parsedDeploymentBuilderFactory;
    }

    public void setParsedDeploymentBuilderFactory(ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory) {
        this.parsedDeploymentBuilderFactory = parsedDeploymentBuilderFactory;
    }

    public FormDefinitionDeploymentHelper getFormDeploymentHelper() {
        return formDeploymentHelper;
    }

    public void setFormDeploymentHelper(FormDefinitionDeploymentHelper formDeploymentHelper) {
        this.formDeploymentHelper = formDeploymentHelper;
    }

    public CachingAndArtifactsManager getCachingAndArtifcatsManager() {
        return cachingAndArtifactsManager;
    }

    public void setCachingAndArtifactsManager(CachingAndArtifactsManager manager) {
        this.cachingAndArtifactsManager = manager;
    }
}
