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
package org.flowable.common.engine.impl.persistence.entity;

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.common.engine.impl.runtime.Clock;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public abstract class AbstractEngineEntityManager<T extends AbstractEngineConfiguration, EntityImpl extends Entity, DM extends DataManager<EntityImpl>>
    extends AbstractEntityManager<EntityImpl, DM> {

    protected T engineConfiguration;

    public AbstractEngineEntityManager(T engineConfiguration, DM dataManager) {
        super(dataManager, engineConfiguration.getEngineCfgKey());
        this.engineConfiguration = engineConfiguration;
    }

    protected T getEngineConfiguration() {
        return engineConfiguration;
    }

    @Override
    protected FlowableEventDispatcher getEventDispatcher() {
        return engineConfiguration.getEventDispatcher();
    }

    protected Clock getClock() {
        return engineConfiguration.getClock();
    }

    protected CommandExecutor getCommandExecutor() {
        return engineConfiguration.getCommandExecutor();
    }

}
