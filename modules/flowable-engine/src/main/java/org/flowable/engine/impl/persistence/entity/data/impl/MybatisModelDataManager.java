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
package org.flowable.engine.impl.persistence.entity.data.impl;

import java.util.List;
import java.util.Map;

import org.flowable.engine.impl.ModelQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ModelEntity;
import org.flowable.engine.impl.persistence.entity.ModelEntityImpl;
import org.flowable.engine.impl.persistence.entity.data.AbstractProcessDataManager;
import org.flowable.engine.impl.persistence.entity.data.ModelDataManager;
import org.flowable.engine.repository.Model;

/**
 * @author Joram Barrez
 */
public class MybatisModelDataManager extends AbstractProcessDataManager<ModelEntity> implements ModelDataManager {

    public MybatisModelDataManager(ProcessEngineConfigurationImpl processEngineConfiguration) {
        super(processEngineConfiguration);
    }

    @Override
    public Class<? extends ModelEntity> getManagedEntityClass() {
        return ModelEntityImpl.class;
    }

    @Override
    public ModelEntity create() {
        return new ModelEntityImpl();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Model> findModelsByQueryCriteria(ModelQueryImpl query) {
        return getDbSqlSession().selectList("selectModelsByQueryCriteria", query);
    }

    @Override
    public long findModelCountByQueryCriteria(ModelQueryImpl query) {
        return (Long) getDbSqlSession().selectOne("selectModelCountByQueryCriteria", query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Model> findModelsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectModelByNativeQuery", parameterMap);
    }

    @Override
    public long findModelCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectModelCountByNativeQuery", parameterMap);
    }

}
