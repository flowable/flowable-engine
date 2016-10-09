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
package org.activiti.idm.engine.impl.persistence.entity;

import org.activiti.idm.api.event.ActivitiIdmEventDispatcher;
import org.activiti.idm.api.event.ActivitiIdmEventType;
import org.activiti.idm.engine.IdmEngineConfiguration;
import org.activiti.idm.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.idm.engine.impl.db.Entity;
import org.activiti.idm.engine.impl.persistence.AbstractManager;
import org.activiti.idm.engine.impl.persistence.entity.data.DataManager;

/**
 * @author Joram Barrez
 */
public abstract class AbstractEntityManager<EntityImpl extends Entity> extends AbstractManager implements EntityManager<EntityImpl> {

  public AbstractEntityManager(IdmEngineConfiguration dmnEngineConfiguration) {
    super(dmnEngineConfiguration);
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
    getDataManager().insert(entity);
  }
  
  @Override
  public void insert(EntityImpl entity, boolean fireCreateEvent) {
    getDataManager().insert(entity);

    ActivitiIdmEventDispatcher eventDispatcher = getEventDispatcher();
    if (fireCreateEvent && eventDispatcher.isEnabled()) {
      eventDispatcher.dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiIdmEventType.ENTITY_CREATED, entity));
      eventDispatcher.dispatchEvent(ActivitiEventBuilder.createEntityEvent(ActivitiIdmEventType.ENTITY_INITIALIZED, entity));
    }
  }
  
  @Override
  public EntityImpl update(EntityImpl entity) {
    EntityImpl updatedEntity = getDataManager().update(entity);
    
    return updatedEntity;
  }
  
  @Override
  public void delete(String id) {
    EntityImpl entity = findById(id);
    delete(entity);
  }
  
  @Override
  public void delete(EntityImpl entity) {
    getDataManager().delete(entity);
  }

  protected abstract DataManager<EntityImpl> getDataManager();

}
