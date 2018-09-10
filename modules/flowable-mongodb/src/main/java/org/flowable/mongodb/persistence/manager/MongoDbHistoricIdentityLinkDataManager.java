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

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.data.HistoricIdentityLinkDataManager;

import com.mongodb.BasicDBObject;

/**
 * @author Tijs Rademakers
 */
public class MongoDbHistoricIdentityLinkDataManager extends AbstractMongoDbDataManager<HistoricIdentityLinkEntity> implements HistoricIdentityLinkDataManager {
    
    public static final String COLLECTION_HISTORIC_IDENTITY_LINKS = "historicIdentityLinks";
    
    @Override
    public String getCollection() {
        return COLLECTION_HISTORIC_IDENTITY_LINKS;
    }

    @Override
    public HistoricIdentityLinkEntity create() {
        return new HistoricIdentityLinkEntityImpl();
    }

    @Override
    public BasicDBObject createUpdateObject(Entity entity) {
        return null;
    }

    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByTaskId(String taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByProcessInstanceId(String processInstanceId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HistoricIdentityLinkEntity> findHistoricIdentityLinksByScopeIdAndScopeType(String scopeId,
            String scopeType) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void deleteHistoricIdentityLinksByScopeIdAndType(String scopeId, String scopeType) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteHistoricIdentityLinksByScopeDefinitionIdAndType(String scopeDefinitionId, String scopeType) {
        // TODO Auto-generated method stub
        
    }

}
