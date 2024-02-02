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

package org.flowable.cmmn.engine.impl.cmd;

import java.io.InputStream;
import java.io.Serializable;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gives access to a deployed case diagram, e.g., a PNG image, through a stream of bytes.
 * 
 * @author Tijs Rademakers
 */
public class GetDeploymentCaseDiagramCmd implements Command<InputStream>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GetDeploymentCaseDiagramCmd.class);

    protected String caseDefinitionId;

    public GetDeploymentCaseDiagramCmd(String caseDefinitionId) {
        if (caseDefinitionId == null || caseDefinitionId.length() == 0) {
            throw new FlowableIllegalArgumentException("The case definition id is mandatory, but '" + caseDefinitionId + "' has been provided.");
        }
        this.caseDefinitionId = caseDefinitionId;
    }

    @Override
    public InputStream execute(CommandContext commandContext) {
        CaseDefinition caseDefinition = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDeploymentManager().findDeployedCaseDefinitionById(caseDefinitionId);
        String deploymentId = caseDefinition.getDeploymentId();
        String resourceName = caseDefinition.getDiagramResourceName();
        if (resourceName == null) {
            LOGGER.info("Resource name is null! No case diagram stream exists.");
            return null;
        } else {
            InputStream caseDiagramStream = new GetDeploymentResourceCmd(deploymentId, resourceName).execute(commandContext);
            return caseDiagramStream;
        }
    }

}
