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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.data.VariableInstanceDataManager;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

/**
 * @author Joram Barrez
 */
public class MongoDbVariableInstanceDataManager extends AbstractMongoDbDataManager implements VariableInstanceDataManager {
    
    public static final String COLLECTION_VARIABLES = "variables";

    @Override
    public VariableInstanceEntity create() {
        return new VariableInstanceEntityImpl();
    }

    @Override
    public VariableInstanceEntity findById(String entityId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insert(VariableInstanceEntity entity) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public VariableInstanceEntity update(VariableInstanceEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String id) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void delete(VariableInstanceEntity entity) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskId(String taskId) {
        FindIterable<Document> variableDocuments = getMongoDbSession().find(COLLECTION_VARIABLES, Filters.eq("taskId", taskId));
        List<VariableInstanceEntity> variableInstanceEntities = new ArrayList<>();
        for (Document variableDocument : variableDocuments) {
            variableInstanceEntities.add(transformToEntity(variableDocument));
        }
        return variableInstanceEntities;
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskIds(Set<String> taskIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionId(String executionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionIds(Set<String> executionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByExecutionAndName(String executionId, String variableName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByExecutionAndNames(String executionId,
            Collection<String> names) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByTaskAndName(String taskId, String variableName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByTaskAndNames(String taskId, Collection<String> names) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstanceByScopeIdAndScopeType(String scopeId, String scopeType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstanceEntity findVariableInstanceByScopeIdAndScopeTypeAndName(String scopeId, String scopeType,
            String variableName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesByScopeIdAndScopeTypeAndNames(String scopeId,
            String scopeType, Collection<String> variableNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstanceBySubScopeIdAndScopeType(String subScopeId,
            String scopeType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VariableInstanceEntity findVariableInstanceBySubScopeIdAndScopeTypeAndName(String subScopeId,
            String scopeType, String variableName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<VariableInstanceEntity> findVariableInstancesBySubScopeIdAndScopeTypeAndNames(String subScopeId,
            String scopeType, Collection<String> variableNames) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteVariablesByTaskId(String taskId) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void deleteVariablesByExecutionId(String executionId) {
        throw new UnsupportedOperationException();        
    }

    @Override
    public void deleteByScopeIdAndScopeType(String scopeId, String scopeType) {
        throw new UnsupportedOperationException();        
    }
    
    public VariableInstanceEntityImpl transformToEntity(Document document) {
        throw new UnsupportedOperationException();
    }

}
