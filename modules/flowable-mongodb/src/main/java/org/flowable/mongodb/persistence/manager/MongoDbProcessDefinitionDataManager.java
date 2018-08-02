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
import org.flowable.engine.impl.ProcessDefinitionQueryImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.ProcessDefinitionDataManager;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * @author Joram Barrez
 */
public class MongoDbProcessDefinitionDataManager extends AbstractMongoDbDataManager implements ProcessDefinitionDataManager {
    
    public static final String COLLECTION_PROCESS_DEFINITIONS = "processDefinitions";

    @Override
    public ProcessDefinitionEntity create() {
        return new ProcessDefinitionEntityImpl();
    }

    @Override
    public ProcessDefinitionEntity findById(String id) {
        FindIterable<Document> result = getMongoDbSession().find(COLLECTION_PROCESS_DEFINITIONS, Filters.eq("_id", id));
        Document processDefinitionDocument = result.first();
        return transformToEntity(processDefinitionDocument);
    }

    @Override
    public void insert(ProcessDefinitionEntity processDefinitionEntity) {
        Document processDefinitionDocument = new Document();
        processDefinitionDocument.append("name", processDefinitionEntity.getName());
        processDefinitionDocument.append("description", processDefinitionEntity.getDescription());
        processDefinitionDocument.append("key", processDefinitionEntity.getKey());
        processDefinitionDocument.append("version", processDefinitionEntity.getVersion());
        processDefinitionDocument.append("category", processDefinitionEntity.getCategory());
        processDefinitionDocument.append("deploymentId", processDefinitionEntity.getDeploymentId());
        processDefinitionDocument.append("resourceName", processDefinitionEntity.getResourceName());
        processDefinitionDocument.append("tenantId", processDefinitionEntity.getTenantId());
        processDefinitionDocument.append("historyLevel", processDefinitionEntity.getHistoryLevel());
        processDefinitionDocument.append("diagramResourceName", processDefinitionEntity.getDiagramResourceName());
        processDefinitionDocument.append("isGraphicalNotationDefined", processDefinitionEntity.isGraphicalNotationDefined());
        processDefinitionDocument.append("hasStartFormKey", processDefinitionEntity.getHasStartFormKey());
        processDefinitionDocument.append("suspensionState", processDefinitionEntity.getSuspensionState());
        processDefinitionDocument.append("derivedFrom", processDefinitionEntity.getDerivedFrom());
        processDefinitionDocument.append("derivedFromRoot", processDefinitionEntity.getDerivedFromRoot());
        processDefinitionDocument.append("derivedVersion", processDefinitionEntity.getDerivedVersion());
        getMongoDbSession().insertOne(processDefinitionEntity, COLLECTION_PROCESS_DEFINITIONS, processDefinitionDocument);
    }

    @Override
    public ProcessDefinitionEntity update(ProcessDefinitionEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void delete(ProcessDefinitionEntity entity) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public ProcessDefinitionEntity findLatestProcessDefinitionByKey(String processDefinitionKey) {
        // TODO. Not all properties included yet. Check the mybatis query for all details.
        // TODO: More performant way possible?
        FindIterable<Document> documents = getMongoDbSession().find(COLLECTION_PROCESS_DEFINITIONS, 
                Filters.eq("key", processDefinitionKey))
                .sort(Sorts.descending("version"))
                .limit(1);
        return transformToEntity(documents.first());
    }

    @Override
    public ProcessDefinitionEntity findLatestProcessDefinitionByKeyAndTenantId(String processDefinitionKey, String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessDefinitionEntity findLatestDerivedProcessDefinitionByKey(String processDefinitionKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessDefinitionEntity findLatestDerivedProcessDefinitionByKeyAndTenantId(String processDefinitionKey, String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteProcessDefinitionsByDeploymentId(String deploymentId) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public List<ProcessDefinition> findProcessDefinitionsByQueryCriteria(ProcessDefinitionQueryImpl processDefinitionQuery) {
        // TODO: extract and do properly
        FindIterable<Document> processDefinitionDocuments = getMongoDbSession().find(COLLECTION_PROCESS_DEFINITIONS, null);
        List<ProcessDefinition> processDefinitions = new ArrayList<>();
        for (Document document : processDefinitionDocuments) {
            processDefinitions.add(transformToEntity(document));
        }
        return processDefinitions;
    }

    @Override
    public long findProcessDefinitionCountByQueryCriteria(ProcessDefinitionQueryImpl processDefinitionQuery) {
        // TODO: extract and do properly
        return getMongoDbSession().count(COLLECTION_PROCESS_DEFINITIONS, null);
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByDeploymentAndKey(String deploymentId, String processDefinitionKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByDeploymentAndKeyAndTenantId(String deploymentId, String processDefinitionKey, String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByKeyAndVersion(String processDefinitionKey, Integer processDefinitionVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionByKeyAndVersionAndTenantId(String processDefinitionKey, Integer processDefinitionVersion, String tenantId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ProcessDefinition> findProcessDefinitionsByNativeQuery(Map<String, Object> parameterMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long findProcessDefinitionCountByNativeQuery(Map<String, Object> parameterMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateProcessDefinitionTenantIdForDeployment(String deploymentId, String newTenantId) {
        throw new UnsupportedOperationException();        
    }
    
    protected ProcessDefinitionEntity transformToEntity(Document document) {
        
        if (document == null) {
            return null;
        }
        
        ProcessDefinitionEntityImpl processDefinitionEntity = new ProcessDefinitionEntityImpl();
        processDefinitionEntity.setId(document.getString("_id"));
        processDefinitionEntity.setName(document.getString("name"));
        processDefinitionEntity.setDescription(document.getString("description"));
        processDefinitionEntity.setKey(document.getString("key"));
        processDefinitionEntity.setVersion(document.getInteger("version", 1));
        processDefinitionEntity.setCategory(document.getString("category"));
        processDefinitionEntity.setDeploymentId(document.getString("deploymentId"));
        processDefinitionEntity.setResourceName(document.getString("resourceName"));
        processDefinitionEntity.setTenantId(document.getString("tenantId"));
        processDefinitionEntity.setHistoryLevel(document.getInteger("historyLevel"));
        processDefinitionEntity.setDiagramResourceName(document.getString("diagramResourceName"));
        processDefinitionEntity.setGraphicalNotationDefined(document.getBoolean("isGraphicalNotationDefined", false));
        processDefinitionEntity.setHasStartFormKey(document.getBoolean("hasStartFormKey"));
        processDefinitionEntity.setSuspensionState(document.getInteger("suspensionState"));
        processDefinitionEntity.setDerivedFrom(document.getString("derivedFrom"));
        processDefinitionEntity.setDerivedFromRoot(document.getString("derivedFromRoot"));
        processDefinitionEntity.setDerivedVersion(document.getInteger("derivedVersion"));
        return processDefinitionEntity;
    }

}
