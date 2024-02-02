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

package org.flowable.entitylink.service.impl.persistence;

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;
import org.flowable.entitylink.service.impl.persistence.entity.EntityLinkEntityManager;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntityManager;

/**
 * @author Tijs Rademakers
 */
public abstract class AbstractManager {
    
    protected EntityLinkServiceConfiguration entityLinkServiceConfiguration;

    public AbstractManager(EntityLinkServiceConfiguration entityLinkServiceConfiguration) {
        this.entityLinkServiceConfiguration = entityLinkServiceConfiguration;
    }

    // Command scoped

    protected CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

    protected <T> T getSession(Class<T> sessionClass) {
        return getCommandContext().getSession(sessionClass);
    }

    // Engine scoped
    
    protected EntityLinkServiceConfiguration getEntityLinkServiceConfiguration() {
        return entityLinkServiceConfiguration;
    }

    protected Clock getClock() {
        return getEntityLinkServiceConfiguration().getClock();
    }

    protected FlowableEventDispatcher getEventDispatcher() {
        return getEntityLinkServiceConfiguration().getEventDispatcher();
    }

    protected EntityLinkEntityManager getEntityLinkEntityManager() {
        return getEntityLinkServiceConfiguration().getEntityLinkEntityManager();
    }

    protected HistoricEntityLinkEntityManager getHistoricEntityLinkEntityManager() {
        return getEntityLinkServiceConfiguration().getHistoricEntityLinkEntityManager();
    }
}
