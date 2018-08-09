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

import java.util.Map;

import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.mongodb.persistence.MongoDbSession;

import com.mongodb.BasicDBObject;

/**
 * @author Joram Barrez
 */
public abstract class AbstractMongoDbDataManager {
    
    protected MongoDbSession getMongoDbSession() {
        return Context.getCommandContext().getSession(MongoDbSession.class);
    }

    public void updateEntity(Entity entity) {
        
    }
    
    protected BasicDBObject setStringUpdateProperty(String propertyName, String value, Map<String, Object> persistentState, BasicDBObject updateObject) {
        if (persistentState.get(propertyName) != null && (String) persistentState.get(propertyName) != value) {
            if (updateObject == null) {
                updateObject = new BasicDBObject();
            }
            updateObject.append(propertyName, value);
        }
        
        return updateObject;
    }
    
    protected BasicDBObject setBooleanUpdateProperty(String propertyName, Boolean value, Map<String, Object> persistentState, BasicDBObject updateObject) {
        if (persistentState.get(propertyName) != null && (Boolean) persistentState.get(propertyName) != value) {
            if (updateObject == null) {
                updateObject = new BasicDBObject();
            }
            updateObject.append(propertyName, value);
        }
        
        return updateObject;
    }
}
