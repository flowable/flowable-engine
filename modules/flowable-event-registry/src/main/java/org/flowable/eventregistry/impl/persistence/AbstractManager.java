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

package org.flowable.eventregistry.impl.persistence;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventregistry.impl.persistence.entity.EventDefinitionEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventDeploymentEntityManager;
import org.flowable.eventregistry.impl.persistence.entity.EventResourceEntityManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public abstract class AbstractManager {

    protected EventRegistryEngineConfiguration eventEngineConfiguration;

    public AbstractManager(EventRegistryEngineConfiguration eventEngineConfiguration) {
        this.eventEngineConfiguration = eventEngineConfiguration;
    }

    // Command scoped

    protected CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

    protected <T> T getSession(Class<T> sessionClass) {
        return getCommandContext().getSession(sessionClass);
    }

    // Engine scoped

    protected EventRegistryEngineConfiguration getEventEngineConfiguration() {
        return eventEngineConfiguration;
    }

    protected EventDeploymentEntityManager getDeploymentEntityManager() {
        return getEventEngineConfiguration().getDeploymentEntityManager();
    }

    protected EventDefinitionEntityManager getFormDefinitionEntityManager() {
        return getEventEngineConfiguration().getEventDefinitionEntityManager();
    }

    protected EventResourceEntityManager getResourceEntityManager() {
        return getEventEngineConfiguration().getResourceEntityManager();
    }

}
