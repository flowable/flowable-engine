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
package org.flowable.ui.modeler.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.ui.common.repository.UuidIdGenerator;
import org.flowable.ui.common.tenant.TenantProvider;
import org.flowable.ui.modeler.domain.ModelHistory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModelHistoryRepositoryImpl implements ModelHistoryRepository {

    private static final String NAMESPACE = "org.flowable.ui.modeler.domain.ModelHistory.";

    @Autowired
    protected SqlSessionTemplate sqlSessionTemplate;

    @Autowired
    protected UuidIdGenerator idGenerator;
    
    @Autowired
    protected TenantProvider tenantProvider;

    @Override
    public ModelHistory get(String id) {
        return sqlSessionTemplate.selectOne(NAMESPACE + "selectModelHistory", id);
    }

    public List<ModelHistory> findByModelTypAndCreatedBy(String createdBy, Integer modelType) {
        Map<String, Object> params = new HashMap<>();
        params.put("modelType", modelType);
        params.put("createdBy", createdBy);
        params.put("tenantId", tenantProvider.getTenantId());
        return sqlSessionTemplate.selectList(NAMESPACE + "selectModelHistoryByTypeAndCreatedBy", params);
    }

    public List<ModelHistory> findByModelId(String modelId) {
        return sqlSessionTemplate.selectList(NAMESPACE + "selectModelHistoryByModelId", modelId);
    }

    @Override
    public void save(ModelHistory modelHistory) {
        modelHistory.setTenantId(tenantProvider.getTenantId());
        if (modelHistory.getId() == null) {
            modelHistory.setId(idGenerator.generateId());
            sqlSessionTemplate.insert(NAMESPACE + "insertModelHistory", modelHistory);
        } else {
            sqlSessionTemplate.update(NAMESPACE + "updateModelHistory", modelHistory);
        }
    }

    @Override
    public void delete(ModelHistory modelHistory) {
        sqlSessionTemplate.delete(NAMESPACE + "deleteModelHistory", modelHistory);
    }

}
