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

package org.flowable.eventregistry.impl.cmd;

import java.io.InputStream;
import java.io.Serializable;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * Gives access to a deployed event model, e.g., an Event definition JSON file, through a stream of bytes.
 * 
 * @author Tijs Rademakers
 */
public class GetEventDefinitionResourceCmd implements Command<InputStream>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String eventDefinitionId;

    public GetEventDefinitionResourceCmd(String eventDefinitionId) {
        if (eventDefinitionId == null || eventDefinitionId.length() < 1) {
            throw new FlowableIllegalArgumentException("The event definition id is mandatory, but '" + eventDefinitionId + "' has been provided.");
        }
        this.eventDefinitionId = eventDefinitionId;
    }

    @Override
    public InputStream execute(CommandContext commandContext) {
        EventDefinitionEntity eventDefinition = CommandContextUtil.getEventRegistryConfiguration().getDeploymentManager()
                .findDeployedEventDefinitionById(eventDefinitionId);

        String deploymentId = eventDefinition.getDeploymentId();
        String resourceName = eventDefinition.getResourceName();
        InputStream eventDefinitionStream = new GetDeploymentResourceCmd(deploymentId, resourceName).execute(commandContext);
        return eventDefinitionStream;
    }

}
