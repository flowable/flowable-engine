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
package org.flowable.common.engine.impl.persistence.cache;

import java.util.HashMap;

import org.flowable.common.engine.impl.persistence.entity.Entity;

/**
 * @author Joram Barrez
 */
public class CachedEntity {

    /**
     * The actual {@link Entity} instance.
     */
    protected Entity entity;

    /**
     * Represents the 'persistence state' at the moment this {@link CachedEntity} instance was created. It is used later on to determine if a {@link Entity} has been updated, by comparing the
     * 'persistent state' at that moment with this instance here.
     */
    protected Object originalPersistentState;

    public CachedEntity(Entity entity, boolean storeState) {
        this.entity = entity;
        if (storeState) {
            this.originalPersistentState = entity.getPersistentState();
            entity.setOriginalPersistentState(originalPersistentState);
        } else if (entity.getOriginalPersistentState() == null){
            entity.setOriginalPersistentState(new HashMap<>(1));
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public Object getOriginalPersistentState() {
        return originalPersistentState;
    }

    public void setOriginalPersistentState(Object originalPersistentState) {
        this.originalPersistentState = originalPersistentState;
    }

    public boolean hasChanged() {
        Object currentPersistentState = entity.getPersistentState();
        return currentPersistentState != null && !entity.getPersistentState().equals(originalPersistentState)
                || currentPersistentState == null && originalPersistentState != null;
    }

}