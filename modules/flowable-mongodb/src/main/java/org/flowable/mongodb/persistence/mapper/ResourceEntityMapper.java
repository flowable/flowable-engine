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
import org.flowable.mongodb.persistence.EntityToDocumentMapper;

/**
 * @author Joram Barrez
 */
public class ResourceEntityMapper extends AbstractEntityToDocumentMapper<ResourceEntityImpl> {

    @Override
    public ResourceEntityImpl fromDocument(Document document) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Document toDocument(ResourceEntityImpl resourceEntity) {
        Document resourceDocument = new Document();
        appendIfNotNull(resourceDocument, "_id", resourceEntity.getId());
        appendIfNotNull(resourceDocument, "name", resourceEntity.getName());
        appendIfNotNull(resourceDocument, "bytes", resourceEntity.getBytes());
        appendIfNotNull(resourceDocument, "deploymentId", resourceEntity.getDeploymentId());
        appendIfNotNull(resourceDocument, "generated", resourceEntity.isGenerated());
        return resourceDocument;
    }

}
