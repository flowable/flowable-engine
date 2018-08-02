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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.flowable.engine.impl.DeploymentQueryImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.DeploymentDataManager;
import org.flowable.engine.repository.Deployment;

import com.mongodb.client.FindIterable;

/**
 * @author Joram Barrez
 */
public class MongoDbDeploymentDataManager extends AbstractMongoDbDataManager implements DeploymentDataManager {
    
    public static final String COLLECTION_DEPLOYMENT = "deployments";

    @Override
    public DeploymentEntity create() {
        return new DeploymentEntityImpl();
    }

    @Override
    public DeploymentEntity findById(String entityId) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void insert(DeploymentEntity deploymentEntity) {
        Document deploymentDocument = new Document();
        deploymentDocument.append("name", deploymentEntity.getName());
        deploymentDocument.append("key", deploymentEntity.getName());
        deploymentDocument.append("parentDeploymentId", deploymentEntity.getParentDeploymentId());
        
        deploymentDocument.append("deploymentTime", deploymentEntity.getDeploymentTime());
        deploymentDocument.append("derivedFrom", deploymentEntity.getDerivedFrom());
        deploymentDocument.append("derivedFromRoot", deploymentEntity.getDerivedFromRoot());

        deploymentDocument.append("tenantId", deploymentEntity.getTenantId());
        
        getMongoDbSession().insertOne(deploymentEntity, COLLECTION_DEPLOYMENT, deploymentDocument);
    }

    @Override
    public DeploymentEntity update(DeploymentEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void delete(DeploymentEntity entity) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public long findDeploymentCountByQueryCriteria(DeploymentQueryImpl deploymentQuery) {
        // TODO: extract and do properly
        return getMongoDbSession().count(COLLECTION_DEPLOYMENT, null);
    }

    @Override
    public List<Deployment> findDeploymentsByQueryCriteria(DeploymentQueryImpl deploymentQuery) {
        // TODO: extract and do properly
        FindIterable<Document> deploymentDocuments = getMongoDbSession().find(COLLECTION_DEPLOYMENT, null);
        List<Deployment> deployments = new ArrayList<>();
        for (Document document : deploymentDocuments) {
            deployments.add(transformToEntity(document));
        }
        return deployments;
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Deployment> findDeploymentsByNativeQuery(Map<String, Object> parameterMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long findDeploymentCountByNativeQuery(Map<String, Object> parameterMap) {
        throw new UnsupportedOperationException();
    }
    
    protected DeploymentEntity transformToEntity(Document document) {
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

}
