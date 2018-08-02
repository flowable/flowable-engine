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
package org.flowable.mongodb.persistence.mapper;

import org.bson.Document;
import org.flowable.engine.impl.persistence.entity.ResourceEntityImpl;
import org.flowable.mongodb.persistence.EntityMapper;

/**
 * @author Joram Barrez
 */
public class ResourceEntityMapper implements EntityMapper<ResourceEntityImpl> {

    @Override
    public ResourceEntityImpl fromDocument(Document document) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Document toDocument(ResourceEntityImpl resourceEntity) {
        Document resourceDocument = new Document();
        resourceDocument.append("_id", resourceEntity.getId());
        resourceDocument.append("name", resourceEntity.getName());
        resourceDocument.append("bytes", resourceEntity.getBytes());
        resourceDocument.append("deploymentId", resourceEntity.getDeploymentId());
        resourceDocument.append("generated", resourceEntity.isGenerated());
        return resourceDocument;
    }

}
