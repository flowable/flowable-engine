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

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.eventregistry.impl.persistence.deploy.EventDefinitionCacheEntry;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntity;
import org.flowable.eventregistry.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SetEventDefinitionCategoryCmd implements Command<Void> {

    protected String eventDefinitionId;
    protected String category;

    public SetEventDefinitionCategoryCmd(String eventDefinitionId, String category) {
        this.eventDefinitionId = eventDefinitionId;
        this.category = category;
    }

    @Override
    public Void execute(CommandContext commandContext) {

        if (eventDefinitionId == null) {
            throw new FlowableIllegalArgumentException("Event definition id is null");
        }

        EventDefinitionEntity eventDefinition = CommandContextUtil.getEventDefinitionEntityManager(commandContext).findById(eventDefinitionId);

        if (eventDefinition == null) {
            throw new FlowableObjectNotFoundException("No event definition found for id = '" + eventDefinitionId + "'");
        }

        // Update category
        eventDefinition.setCategory(category);

        // Remove form from cache, it will be refetch later
        DeploymentCache<EventDefinitionCacheEntry> eventDefinitionCache = CommandContextUtil.getEventRegistryConfiguration().getEventDefinitionCache();
        if (eventDefinitionCache != null) {
            eventDefinitionCache.remove(eventDefinitionId);
        }

        CommandContextUtil.getEventDefinitionEntityManager(commandContext).update(eventDefinition);

        return null;
    }

    public String getEventDefinitionId() {
        return eventDefinitionId;
    }

    public void setEventDefinitionId(String eventDefinitionId) {
        this.eventDefinitionId = eventDefinitionId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

}
