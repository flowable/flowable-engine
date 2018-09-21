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
package org.flowable.mongodb.persistence.manager;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.mongodb.persistence.MongoDbSession;

import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;

/**
 * @author Joram Barrez
 */
public abstract class AbstractMongoDbDataManager<EntityImpl extends Entity> implements DataManager<EntityImpl> {
    
    public abstract String getCollection();
    
    protected MongoDbSession getMongoDbSession() {
        return Context.getCommandContext().getSession(MongoDbSession.class);
    }
    
    @Override
    public EntityImpl findById(String id) {
        return getMongoDbSession().findOne(getCollection(), id);
    }

    @Override
    public void insert(EntityImpl entity) {
        getMongoDbSession().insertOne(entity);
    }
    
    @Override
    public EntityImpl update(EntityImpl entity) {
        getMongoDbSession().update(entity);
        return entity;
    }
    
    @Override
    public void delete(String id) {
        EntityImpl entity = findById(id);
        delete(entity);
    }

    @Override
    public void delete(EntityImpl entity) {
        getMongoDbSession().delete(getCollection(), entity);    
    }
    
    /**
     * Implements the update logic for the specific {@link Entity} managed and returns a {@link BasicDBObject} representing the changes.
     * 
     * Contrary to the relational counterpart, the update is not generic and thus each subclass needs to implement the actual update. 
     * (The specific part for the relational case is in the Mybatis xml, so it's implicit and still needed to be written)
     */
    public abstract BasicDBObject createUpdateObject(Entity entity);
    
    /**
     * Helper method for subclasses to create a {@link BasicDBObject} that can be used to execute an update to an {@link Entity}.
     */
    @SuppressWarnings("unchecked")
    protected BasicDBObject setUpdateProperty(Entity entity, String propertyName, Object value, BasicDBObject updateObject) {
        Map<String, Object> persistentState = (Map<String, Object>) entity.getOriginalPersistentState();
        if ((persistentState.get(propertyName) == null && value != null) || // value didn't exist before
            (persistentState.get(propertyName) != null && !persistentState.get(propertyName).equals(value))) { // value existed and is changed
            
            if (updateObject == null) {
                updateObject = new BasicDBObject();
            }
            updateObject.append(propertyName, value);
        }
        
        return updateObject;
    }

    protected Bson makeAndFilter(List<Bson> filters) {
        if (filters.size() > 1) {
            return Filters.and(filters);
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            return new Document();
        }
    }

}
