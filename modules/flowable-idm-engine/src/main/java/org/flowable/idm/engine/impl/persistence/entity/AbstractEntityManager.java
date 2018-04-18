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

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.persistence.entity.EntityManager;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.idm.api.event.FlowableIdmEventType;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.delegate.event.impl.FlowableIdmEventBuilder;
import org.flowable.idm.engine.impl.persistence.AbstractManager;

/**
 * @author Joram Barrez
 */
public abstract class AbstractEntityManager<EntityImpl extends Entity> extends AbstractManager implements EntityManager<EntityImpl> {

    public AbstractEntityManager(IdmEngineConfiguration idmEngineConfiguration) {
        super(idmEngineConfiguration);
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
        if (entity instanceof HasRevision) {
            ((HasRevision) entity).setRevision(((HasRevision) entity).getRevisionNext());
        }

        getDataManager().insert(entity);

        FlowableEventDispatcher eventDispatcher = getEventDispatcher();
        if (fireCreateEvent && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableIdmEventBuilder.createEntityEvent(FlowableIdmEventType.ENTITY_CREATED, entity));
            eventDispatcher.dispatchEvent(FlowableIdmEventBuilder.createEntityEvent(FlowableIdmEventType.ENTITY_INITIALIZED, entity));
        }
    }

    @Override
    public EntityImpl update(EntityImpl entity) {
        return update(entity, true);
    }

    @Override
    public EntityImpl update(EntityImpl entity, boolean fireUpdateEvent) {
        EntityImpl updatedEntity = getDataManager().update(entity);

        if (fireUpdateEvent && getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableIdmEventBuilder.createEntityEvent(FlowableIdmEventType.ENTITY_UPDATED, entity));
        }

        return updatedEntity;
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
            getEventDispatcher().dispatchEvent(FlowableIdmEventBuilder.createEntityEvent(FlowableIdmEventType.ENTITY_DELETED, entity));
        }
    }

    protected abstract DataManager<EntityImpl> getDataManager();

}
