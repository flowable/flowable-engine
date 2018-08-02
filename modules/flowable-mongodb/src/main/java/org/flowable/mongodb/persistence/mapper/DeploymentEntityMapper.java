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
import org.flowable.engine.impl.persistence.entity.DeploymentEntityImpl;
import org.flowable.mongodb.persistence.EntityMapper;

public class DeploymentEntityMapper implements EntityMapper<DeploymentEntityImpl> {

    @Override
    public DeploymentEntityImpl fromDocument(Document document) {
        DeploymentEntityImpl deploymentEntity = new DeploymentEntityImpl();
        deploymentEntity.setId(document.getString("_id"));
        deploymentEntity.setName(document.getString("name"));
        deploymentEntity.setKey(document.getString("key"));
        deploymentEntity.setParentDeploymentId(document.getString("parentDeployment"));
        deploymentEntity.setDeploymentTime(document.getDate("deploymentTime"));
        deploymentEntity.setDerivedFrom(document.getString("derivedFrom"));
        deploymentEntity.setDerivedFromRoot(document.getString("derivedFromRoot"));
        deploymentEntity.setTenantId(document.getString("tenantId"));
        return deploymentEntity;
    }
    
    @Override
    public Document toDocument(DeploymentEntityImpl deploymentEntity) {
        Document deploymentDocument = new Document();
        deploymentDocument.append("_id", deploymentEntity.getId());
        deploymentDocument.append("name", deploymentEntity.getName());
        deploymentDocument.append("key", deploymentEntity.getName());
        deploymentDocument.append("parentDeploymentId", deploymentEntity.getParentDeploymentId());
        
        deploymentDocument.append("deploymentTime", deploymentEntity.getDeploymentTime());
        deploymentDocument.append("derivedFrom", deploymentEntity.getDerivedFrom());
        deploymentDocument.append("derivedFromRoot", deploymentEntity.getDerivedFromRoot());

        deploymentDocument.append("tenantId", deploymentEntity.getTenantId());
        return deploymentDocument;
    }

}
