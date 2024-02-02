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

package org.flowable.identitylink.service.impl.persistence;

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityManager;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityManager;

/**
 * @author Tijs Rademakers
 */
public abstract class AbstractManager {
    
    protected IdentityLinkServiceConfiguration identityLinkServiceConfiguration;

    public AbstractManager(IdentityLinkServiceConfiguration identityLinkServiceConfiguration) {
        this.identityLinkServiceConfiguration = identityLinkServiceConfiguration;
    }

    // Command scoped

    protected CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

    protected <T> T getSession(Class<T> sessionClass) {
        return getCommandContext().getSession(sessionClass);
    }

    // Engine scoped
    
    protected IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return identityLinkServiceConfiguration;
    }

    protected Clock getClock() {
        return getIdentityLinkServiceConfiguration().getClock();
    }

    protected FlowableEventDispatcher getEventDispatcher() {
        return getIdentityLinkServiceConfiguration().getEventDispatcher();
    }

    protected IdentityLinkEntityManager getIdentityLinkEntityManager() {
        return getIdentityLinkServiceConfiguration().getIdentityLinkEntityManager();
    }

    protected HistoricIdentityLinkEntityManager getHistoricIdentityLinkEntityManager() {
        return getIdentityLinkServiceConfiguration().getHistoricIdentityLinkEntityManager();
    }
}
