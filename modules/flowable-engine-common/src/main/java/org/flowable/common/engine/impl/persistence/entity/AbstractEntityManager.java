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

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.event.FlowableEntityEventImpl;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public abstract class AbstractEntityManager<EntityImpl extends Entity, DM extends DataManager<EntityImpl>>
    implements EntityManager<EntityImpl> {

    protected DM dataManager;
    protected String engineType;

    public AbstractEntityManager(DM dataManager, String engineType) {
        this.dataManager = dataManager;
        this.engineType = engineType;
    }

    /*
     * CRUD operations
     */

    @Override
    public EntityImpl findById(String entityId) {
        return getDataManager().findById(entityId);
    }

    @Override
    public EntityImpl create() {
        return getDataManager().create();
    }

    @Override
    public void insert(EntityImpl entity) {
        insert(entity, true);
    }

    @Override
    public void insert(EntityImpl entity, boolean fireCreateEvent) {
        getDataManager().insert(entity);
        if (fireCreateEvent) {
            fireEntityInsertedEvent(entity);
        }
    }

    protected void fireEntityInsertedEvent(Entity entity) {
        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, entity), engineType);
            eventDispatcher.dispatchEvent(createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, entity), engineType);
        }
    }

    @Override
    public EntityImpl update(EntityImpl entity) {
        return update(entity, true);
    }

    @Override
    public EntityImpl update(EntityImpl entity, boolean fireUpdateEvent) {
        EntityImpl updatedEntity = getDataManager().update(entity);
        if (fireUpdateEvent) {
            fireEntityUpdatedEvent(entity);
        }
        return updatedEntity;
    }

    protected void fireEntityUpdatedEvent(Entity entity) {
        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            getEventDispatcher().dispatchEvent(createEntityEvent(FlowableEngineEventType.ENTITY_UPDATED, entity), engineType);
        }
    }

    @Override
    public void delete(String id) {
        EntityImpl entity = findById(id);
        delete(entity);
    }

    @Override
    public void delete(EntityImpl entity) {
        delete(entity, true);
    }

    @Override
    public void delete(EntityImpl entity, boolean fireDeleteEvent) {
        getDataManager().delete(entity);

        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (fireDeleteEvent && eventDispatcher != null && eventDispatcher.isEnabled()) {
            fireEntityDeletedEvent(entity);
        }
    }

    protected void fireEntityDeletedEvent(Entity entity) {
        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, entity), engineType);
        }
    }

    protected FlowableEntityEvent createEntityEvent(FlowableEngineEventType eventType, Entity entity) {
        return new FlowableEntityEventImpl(entity, eventType);
    }

    protected DM getDataManager() {
        return dataManager;
    }

    protected void setDataManager(DM dataManager) {
        this.dataManager = dataManager;
    }

    protected abstract FlowableEventDispatcher getEventDispatcher();

}
