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
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.AbstractManager;

/**
 * @author Joram Barrez
 */
public abstract class AbstractEntityManager<EntityImpl extends Entity> extends AbstractManager implements EntityManager<EntityImpl> {

    public AbstractEntityManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
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
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_CREATED, entity));
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_INITIALIZED, entity));
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
            getEventDispatcher().dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_UPDATED, entity));
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

        if (fireDeleteEvent && getEventDispatcher().isEnabled()) {
            fireEntityDeletedEvent(entity);
        }
    }

    protected void fireEntityDeletedEvent(Entity entity) {
        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableEventBuilder.createEntityEvent(FlowableEngineEventType.ENTITY_DELETED, entity));
        }
    }

    protected abstract DataManager<EntityImpl> getDataManager();
}
