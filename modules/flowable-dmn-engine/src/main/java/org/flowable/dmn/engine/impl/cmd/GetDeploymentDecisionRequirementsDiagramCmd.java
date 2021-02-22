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

package org.flowable.dmn.engine.impl.cmd;

import java.io.InputStream;
import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class GetDeploymentDecisionRequirementsDiagramCmd implements Command<InputStream>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GetDeploymentDecisionRequirementsDiagramCmd.class);

    protected String decisionId;

    public GetDeploymentDecisionRequirementsDiagramCmd(String decisionId) {
        if (decisionId == null || decisionId.length() == 0) {
            throw new FlowableIllegalArgumentException("The decision id is mandatory, but '" + decisionId + "' has been provided.");
        }
        this.decisionId = decisionId;
    }

    @Override
    public InputStream execute(CommandContext commandContext) {
        DecisionEntity decisionEntity = CommandContextUtil.getDmnEngineConfiguration(commandContext).getDeploymentManager().findDeployedDecisionById(decisionId);
        String deploymentId = decisionEntity.getDeploymentId();
        String resourceName = decisionEntity.getDiagramResourceName();
        if (resourceName == null) {
            LOGGER.info("Resource name is null! No decision requirements diagram stream exists.");
            return null;
        } else {
            return new GetDeploymentResourceCmd(deploymentId, resourceName).execute(commandContext);
        }
    }
}