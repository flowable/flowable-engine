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

import java.util.LinkedHashMap;
import java.util.Map;

import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.persistence.deploy.Deployer;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntityManager;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 * @author Yvo Swillens
 */
public class DmnDeployer implements Deployer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DmnDeployer.class);

    protected IdGenerator idGenerator;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected DmnDeploymentHelper dmnDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;
    protected DecisionRequirementsDiagramHelper decisionRequirementsDiagramHelper;
    protected boolean usePrefixId;

    @Override
    public void deploy(DmnDeploymentEntity deployment, Map<String, Object> deploymentSettings) {
        LOGGER.debug("Processing deployment {}", deployment.getName());

        // The ParsedDeployment represents the deployment, the decision and the DMN
        // resource, parse, and model associated with each decision table.
        ParsedDeployment parsedDeployment = parsedDeploymentBuilderFactory.getBuilderForDeploymentAndSettings(deployment, deploymentSettings).build();

        dmnDeploymentHelper.copyDeploymentValuesToDecisions(parsedDeployment.getDeployment(), parsedDeployment.getAllDecisions());
        dmnDeploymentHelper.setResourceNamesOnDecisions(parsedDeployment);

        dmnDeploymentHelper.createAndPersistNewDiagramsIfNeeded(parsedDeployment, decisionRequirementsDiagramHelper);
        dmnDeploymentHelper.setDecisionDefinitionDiagramNames(parsedDeployment);

        if (deployment.isNew()) {
            Map<DecisionEntity, DecisionEntity> mapOfNewDefinitionToPreviousVersion = getPreviousVersionsOfDecisions(parsedDeployment);
            setDecisionVersionsAndIds(parsedDeployment, mapOfNewDefinitionToPreviousVersion);
            persistDecisions(parsedDeployment);
        } else {
            makeDecisionsConsistentWithPersistedVersions(parsedDeployment);
        }

        cachingAndArtifactsManager.updateCachingAndArtifacts(parsedDeployment);
    }

    /**
     * Constructs a map from new DecisionEntities to the previous version by key and tenant. If no previous version exists, no map entry is created.
     */
    protected Map<DecisionEntity, DecisionEntity> getPreviousVersionsOfDecisions(ParsedDeployment parsedDeployment) {

        Map<DecisionEntity, DecisionEntity> result = new LinkedHashMap<>();

        for (DecisionEntity newDecision : parsedDeployment.getAllDecisions()) {
            DecisionEntity existingDecision = dmnDeploymentHelper.getMostRecentVersionOfDecision(newDecision);

            if (existingDecision != null) {
                result.put(newDecision, existingDecision);
            }
        }

        return result;
    }

    /**
     * Sets the version on each decision entity, and the identifier. If the map contains an older version for a decision, then the version is set to that older entity's version plus one;
     * otherwise it is set to 1.
     */
    protected void setDecisionVersionsAndIds(ParsedDeployment parsedDeployment, Map<DecisionEntity, DecisionEntity> mapNewToOldDecisions) {

        for (DecisionEntity decision : parsedDeployment.getAllDecisions()) {
            int version = 1;

            DecisionEntity latest = mapNewToOldDecisions.get(decision);
            if (latest != null) {
                version = latest.getVersion() + 1;
            }

            decision.setVersion(version);
            if (usePrefixId) {
                decision.setId(decision.getIdPrefix() + idGenerator.getNextId());
            } else {
                String id = idGenerator.getNextId();
                decision.setId(id);
            }
        }
    }

    /**
     * Saves each decision. It is assumed that the deployment is new, the decisions have never been saved before, and that they have all their values properly set up.
     */
    protected void persistDecisions(ParsedDeployment parsedDeployment) {
        DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration();
        DecisionEntityManager decisionEntityManager = dmnEngineConfiguration.getDecisionEntityManager();

        for (DecisionEntity decision : parsedDeployment.getAllDecisions()) {
            decisionEntityManager.insert(decision);
        }
    }

    /**
     * Loads the persisted version of each decision and set values on the in-memory version to be consistent.
     */
    protected void makeDecisionsConsistentWithPersistedVersions(ParsedDeployment parsedDeployment) {
        for (DecisionEntity decision : parsedDeployment.getAllDecisions()) {
            DecisionEntity persistedDecision = dmnDeploymentHelper.getPersistedInstanceOfDecision(decision);

            if (persistedDecision != null) {
                decision.setId(persistedDecision.getId());
                decision.setVersion(persistedDecision.getVersion());
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

    public DmnDeploymentHelper getDmnDeploymentHelper() {
        return dmnDeploymentHelper;
    }

    public void setDmnDeploymentHelper(DmnDeploymentHelper dmnDeploymentHelper) {
        this.dmnDeploymentHelper = dmnDeploymentHelper;
    }

    public CachingAndArtifactsManager getCachingAndArtifcatsManager() {
        return cachingAndArtifactsManager;
    }

    public void setCachingAndArtifactsManager(CachingAndArtifactsManager manager) {
        this.cachingAndArtifactsManager = manager;
    }

    public boolean isUsePrefixId() {
        return usePrefixId;
    }

    public void setUsePrefixId(boolean usePrefixId) {
        this.usePrefixId = usePrefixId;
    }

    public DecisionRequirementsDiagramHelper getDecisionRequirementsDiagramHelper() {
        return decisionRequirementsDiagramHelper;
    }

    public void setDecisionRequirementsDiagramHelper(DecisionRequirementsDiagramHelper decisionRequirementsDiagramHelper) {
        this.decisionRequirementsDiagramHelper = decisionRequirementsDiagramHelper;
    }
}
