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
package org.flowable.app.engine.impl.cmd;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntity;
import org.flowable.app.engine.impl.persistence.entity.AppResourceEntity;
import org.flowable.app.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Tijs Rademakers
 */
public class GetDeploymentResourceCmd implements Command<InputStream> {

    protected String deploymentId;
    protected String resourceName;

    public GetDeploymentResourceCmd(String deploymentId, String resourceName) {
        this.deploymentId = deploymentId;
        this.resourceName = resourceName;
    }

    @Override
    public InputStream execute(CommandContext commandContext) {
        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("deploymentId is null");
        }
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("resourceName is null");
        }

        AppResourceEntity resource = CommandContextUtil.getAppResourceEntityManager(commandContext)
                .findResourceByDeploymentIdAndResourceName(deploymentId, resourceName);
        if (resource == null) {
            if (CommandContextUtil.getAppDeploymentEntityManager(commandContext).findById(deploymentId) == null) {
                throw new FlowableObjectNotFoundException("deployment does not exist: " + deploymentId, AppDeploymentEntity.class);
            } else {
                throw new FlowableObjectNotFoundException("no resource found with name '" + resourceName + "' in deployment '" + deploymentId + "'", AppResourceEntity.class);
            }
        }
        return new ByteArrayInputStream(resource.getBytes());
    }

}
