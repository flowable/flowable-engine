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

import org.bson.Document;
import org.flowable.engine.impl.persistence.entity.ResourceEntity;
import org.flowable.engine.impl.persistence.entity.ResourceEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.ResourceDataManager;

/**
 * @author Joram Barrez
 */
public class MongoDbResourceDataManager extends AbstractMongoDbDataManager implements ResourceDataManager {
    
    public static final String COLLECTION_BYTE_ARRAY = "byteArrays";

    @Override
    public ResourceEntity create() {
        return new ResourceEntityImpl();
    }

    @Override
    public ResourceEntity findById(String entityId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(ResourceEntity resourceEntity) {
        Document resourceDocument = new Document();
        resourceDocument.append("name", resourceEntity.getName());
        resourceDocument.append("bytes", resourceEntity.getBytes());
        resourceDocument.append("deploymentId", resourceEntity.getDeploymentId());
        resourceDocument.append("generated", resourceEntity.isGenerated());
        
        getMongoDbSession().insertOne(resourceEntity, COLLECTION_BYTE_ARRAY, resourceDocument);
    }

    @Override
    public ResourceEntity update(ResourceEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void delete(ResourceEntity entity) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public ResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        throw new UnsupportedOperationException();
    }

}
