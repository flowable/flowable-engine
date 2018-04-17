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
package org.flowable.cmmn.engine.impl.deployer;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.api.repository.EngineDeployment;
import org.flowable.common.engine.impl.util.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates diagrams from case definitions.
 */
public class CaseDefinitionDiagramHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDefinitionDiagramHelper.class);

    /**
     * Generates a diagram resource for a CaseDefinitionEntity. The returned resource has not yet been persisted, nor attached to the CaseDefinitionEntity. This requires
     * that the CaseDefinitionEntity have its key and resource name already set.
     * 
     * The caller must determine whether creating a diagram for this case definition is appropriate or not, for example see {@link #shouldCreateDiagram(CaseDefinitionEntity, EngineDeployment)}.
     */
    public CmmnResourceEntity createDiagramForCaseDefinition(CaseDefinitionEntity caseDefinition, CmmnModel cmmnModel) {

        if (StringUtils.isEmpty(caseDefinition.getKey()) || StringUtils.isEmpty(caseDefinition.getResourceName())) {
            throw new IllegalStateException("Provided case definition must have both key and resource name set.");
        }

        CmmnResourceEntity resource = createResourceEntity();
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        try {
            byte[] diagramBytes = IoUtil.readInputStream(
                            cmmnEngineConfiguration.getCaseDiagramGenerator().generateDiagram(cmmnModel, "png",
                                            cmmnEngineConfiguration.getActivityFontName(),
                                            cmmnEngineConfiguration.getLabelFontName(),
                                            cmmnEngineConfiguration.getAnnotationFontName(),
                                            cmmnEngineConfiguration.getClassLoader()), null);
            String diagramResourceName = ResourceNameUtil.getCaseDiagramResourceName(
                            caseDefinition.getResourceName(), caseDefinition.getKey(), "png");

            resource.setName(diagramResourceName);
            resource.setBytes(diagramBytes);
            resource.setDeploymentId(caseDefinition.getDeploymentId());

            // Mark the resource as 'generated'
            resource.setGenerated(true);

        } catch (Throwable t) { // if anything goes wrong, we don't store the image (the case will still be executable).
            LOGGER.warn("Error while generating case diagram, image will not be stored in repository", t);
            resource = null;
        }

        return resource;
    }

    protected CmmnResourceEntity createResourceEntity() {
        return CommandContextUtil.getCmmnEngineConfiguration().getCmmnResourceEntityManager().create();
    }

    public boolean shouldCreateDiagram(CaseDefinitionEntity caseDefinition, EngineDeployment deployment) {
        if (deployment.isNew() && caseDefinition.hasGraphicalNotation()
                && CommandContextUtil.getCmmnEngineConfiguration().isCreateDiagramOnDeploy()) {

            // If the 'getProcessDiagramResourceNameFromDeployment' call returns null, it means
            // no diagram image for the process definition was provided in the deployment resources.
            return ResourceNameUtil.getCaseDiagramResourceNameFromDeployment(caseDefinition, deployment.getResources()) == null;
        }

        return false;
    }
}
