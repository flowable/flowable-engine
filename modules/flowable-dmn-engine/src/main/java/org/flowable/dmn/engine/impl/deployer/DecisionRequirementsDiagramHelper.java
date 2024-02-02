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

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.repository.EngineDeployment;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntity;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.DmnDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates diagrams from decision definitions.
 */
public class DecisionRequirementsDiagramHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecisionRequirementsDiagramHelper.class);

    /**
     * Generates a diagram resource for a DecisionEntity. The returned resource has not yet been persisted, nor attached to the CaseDefinitionEntity. This requires
     * that the DecisionEntity have its key and resource name already set.
     * <p>
     * The caller must determine whether creating a diagram for this decision is appropriate or not, for example see {@link #shouldCreateDiagram}.
     */
    public DmnResourceEntity createDiagramForDecision(DecisionEntity decision, DmnDefinition dmnDefinition) {

        if (StringUtils.isEmpty(decision.getKey()) || StringUtils.isEmpty(decision.getResourceName())) {
            throw new IllegalStateException("Provided decision definition must have both key and resource name set.");
        }

        DmnResourceEntity resource = createResourceEntity();
        DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration();
        try {
            byte[] diagramBytes = IoUtil.readInputStream(
                    dmnEngineConfiguration.getDecisionRequirementsDiagramGenerator().generateDiagram(dmnDefinition, "png",
                            dmnEngineConfiguration.getDecisionFontName(),
                            dmnEngineConfiguration.getLabelFontName(),
                            dmnEngineConfiguration.getAnnotationFontName(),
                            dmnEngineConfiguration.getClassLoader()), null);
            String diagramResourceName = ResourceNameUtil.getDecisionRequirementsDiagramResourceName(
                    decision.getResourceName(), decision.getKey(), "png");

            resource.setName(diagramResourceName);
            resource.setBytes(diagramBytes);
            resource.setDeploymentId(decision.getDeploymentId());

            // Mark the resource as 'generated'
            resource.setGenerated(true);

        } catch (Throwable t) { // if anything goes wrong, we don't store the image (the decision will still be executable).
            LOGGER.warn("Error while generating decision requirements diagram, image will not be stored in repository", t);
            resource = null;
        }

        return resource;
    }

    protected DmnResourceEntity createResourceEntity() {
        return CommandContextUtil.getDmnEngineConfiguration().getResourceEntityManager().create();
    }

    public boolean shouldCreateDiagram(DecisionEntity decision, EngineDeployment deployment) {
        if (deployment.isNew() && decision.hasGraphicalNotation()
                && CommandContextUtil.getDmnEngineConfiguration().isCreateDiagramOnDeploy()) {

            // If the 'getDecisionRequirementsDiagramResourceNameFromDeployment' call returns null, it means
            // no diagram image for the decision was provided in the deployment resources.
            return ResourceNameUtil.getDecisionRequirementsDiagramResourceNameFromDeployment(decision, deployment.getResources()) == null;
        }

        return false;
    }
}
