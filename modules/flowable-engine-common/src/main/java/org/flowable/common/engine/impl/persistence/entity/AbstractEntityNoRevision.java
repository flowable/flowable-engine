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

/**
 * Abstract superclass for the common properties of all {@link Entity} implementations.
 * 
 * @author Joram Barrez
 */
public abstract class AbstractEntityNoRevision implements Entity {

    protected String id;

    protected boolean isInserted;
    protected boolean isUpdated;
    protected boolean isDeleted;
    
    protected Object originalPersistentState;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean isInserted() {
        return isInserted;
    }

    @Override
    public void setInserted(boolean isInserted) {
        this.isInserted = isInserted;
    }

    @Override
    public boolean isUpdated() {
        return isUpdated;
    }

    @Override
    public void setUpdated(boolean isUpdated) {
        this.isUpdated = isUpdated;
    }

    @Override
    public boolean isDeleted() {
        return isDeleted;
    }

    @Override
    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    @Override
    public Object getOriginalPersistentState() {
        return originalPersistentState;
    }

    @Override
    public void setOriginalPersistentState(Object persistentState) {
        this.originalPersistentState = persistentState;
    }
}
