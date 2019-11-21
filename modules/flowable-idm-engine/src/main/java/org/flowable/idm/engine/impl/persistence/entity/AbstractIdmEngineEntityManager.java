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
package org.flowable.idm.engine.impl.persistence.entity;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.idm.api.event.FlowableIdmEventType;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.delegate.event.impl.FlowableIdmEventBuilder;

/**
 * @author Joram Barrez
 */
public abstract class AbstractIdmEngineEntityManager<EntityImpl extends Entity, DM extends DataManager<EntityImpl>>
    extends AbstractEngineEntityManager<IdmEngineConfiguration, EntityImpl, DM> {

    public AbstractIdmEngineEntityManager(IdmEngineConfiguration idmEngineConfiguration, DM dataManager) {
        super(idmEngineConfiguration, dataManager);
    }

    @Override
    protected FlowableEntityEvent createEntityEvent(FlowableEngineEventType eventType, Entity entity) {
        FlowableIdmEventType idmEventType;
        switch (eventType) {
            case ENTITY_CREATED:
                idmEventType = FlowableIdmEventType.ENTITY_CREATED;
                break;
            case ENTITY_INITIALIZED:
                idmEventType = FlowableIdmEventType.ENTITY_INITIALIZED;
                break;
            case ENTITY_UPDATED:
                idmEventType = FlowableIdmEventType.ENTITY_UPDATED;
                break;
            case ENTITY_DELETED:
                idmEventType = FlowableIdmEventType.ENTITY_DELETED;
                break;
            default:
                idmEventType = null;
        }

        if (idmEventType != null) {
            return FlowableIdmEventBuilder.createEntityEvent(idmEventType, entity);
        } else {
            return super.createEntityEvent(eventType, entity);
        }
    }
}
