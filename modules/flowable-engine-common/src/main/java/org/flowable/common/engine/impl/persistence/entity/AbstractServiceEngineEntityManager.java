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
import org.flowable.common.engine.impl.AbstractServiceConfiguration;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.common.engine.impl.runtime.Clock;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public abstract class AbstractServiceEngineEntityManager<T extends AbstractServiceConfiguration, EntityImpl extends Entity, DM extends DataManager<EntityImpl>>
    extends AbstractEntityManager<EntityImpl, DM> {

    protected T serviceConfiguration;

    public AbstractServiceEngineEntityManager(T serviceConfiguration, String engineType, DM dataManager) {
        super(dataManager, engineType);
        this.serviceConfiguration = serviceConfiguration;
    }

    protected T getServiceConfiguration() {
        return serviceConfiguration;
    }

    @Override
    protected FlowableEventDispatcher getEventDispatcher() {
        return serviceConfiguration.getEventDispatcher();
    }

    protected Clock getClock() {
        return serviceConfiguration.getClock();
    }
}
