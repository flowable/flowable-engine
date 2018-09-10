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

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.engine.impl.DeploymentQueryImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntity;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.DeploymentDataManager;
import org.flowable.engine.repository.Deployment;

import com.mongodb.BasicDBObject;

/**
 * @author Joram Barrez
 */
public class MongoDbDeploymentDataManager extends AbstractMongoDbDataManager<DeploymentEntity> implements DeploymentDataManager {
    
    public static final String COLLECTION_DEPLOYMENT = "deployments";

    @Override
    public String getCollection() {
        return COLLECTION_DEPLOYMENT;
    }
    
    @Override
    public DeploymentEntity create() {
        return new DeploymentEntityImpl();
    }
    
    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        return null;
    }

    @Override
    public long findDeploymentCountByQueryCriteria(DeploymentQueryImpl deploymentQuery) {
        // TODO: extract and do properly
        return getMongoDbSession().count(COLLECTION_DEPLOYMENT, null);
    }

    @Override
    public List<Deployment> findDeploymentsByQueryCriteria(DeploymentQueryImpl deploymentQuery) {
        return getMongoDbSession().find(COLLECTION_DEPLOYMENT, null);
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
    
}
