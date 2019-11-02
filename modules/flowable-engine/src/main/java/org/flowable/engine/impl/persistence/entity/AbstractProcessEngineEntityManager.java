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
package org.flowable.engine.impl.persistence.entity;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;

/**
 * @author Joram Barrez
 */
public abstract class AbstractProcessEngineEntityManager<EntityImpl extends Entity, DM extends DataManager<EntityImpl>>
    extends AbstractEngineEntityManager<ProcessEngineConfigurationImpl, EntityImpl, DM> {

    public AbstractProcessEngineEntityManager(ProcessEngineConfigurationImpl processEngineConfiguration, DM dataManager) {
        super(processEngineConfiguration, dataManager);
    }

    @Override
    protected FlowableEntityEvent createEntityEvent(FlowableEngineEventType eventType, Entity entity) {
        return FlowableEventBuilder.createEntityEvent(eventType, entity);
    }
}
