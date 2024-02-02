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

package org.flowable.idm.engine.impl.persistence;

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.IdentityInfoEntityManager;
import org.flowable.idm.engine.impl.persistence.entity.MembershipEntityManager;
import org.flowable.idm.engine.impl.util.CommandContextUtil;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public abstract class AbstractManager {

    protected IdmEngineConfiguration idmEngineConfiguration;

    public AbstractManager(IdmEngineConfiguration idmEngineConfiguration) {
        this.idmEngineConfiguration = idmEngineConfiguration;
    }

    // Command scoped

    protected CommandContext getCommandContext() {
        return CommandContextUtil.getCommandContext();
    }

    protected <T> T getSession(Class<T> sessionClass) {
        return getCommandContext().getSession(sessionClass);
    }

    // Engine scoped

    protected IdmEngineConfiguration getIdmEngineConfiguration() {
        return idmEngineConfiguration;
    }

    protected CommandExecutor getCommandExecutor() {
        return getIdmEngineConfiguration().getCommandExecutor();
    }

    protected FlowableEventDispatcher getEventDispatcher() {
        return getIdmEngineConfiguration().getEventDispatcher();
    }

    protected GroupEntityManager getGroupEntityManager() {
        return getIdmEngineConfiguration().getGroupEntityManager();
    }

    protected MembershipEntityManager getMembershipEntityManager() {
        return getIdmEngineConfiguration().getMembershipEntityManager();
    }

    protected IdentityInfoEntityManager getIdentityInfoEntityManager() {
        return getIdmEngineConfiguration().getIdentityInfoEntityManager();
    }

}
