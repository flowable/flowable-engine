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
package org.flowable.form.engine.impl.persistence.entity.data.impl;

import java.util.List;
import java.util.Map;

import org.flowable.form.api.FormDeployment;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormDeploymentQueryImpl;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntity;
import org.flowable.form.engine.impl.persistence.entity.FormDeploymentEntityImpl;
import org.flowable.form.engine.impl.persistence.entity.data.AbstractFormDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.FormDeploymentDataManager;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class MybatisFormDeploymentDataManager extends AbstractFormDataManager<FormDeploymentEntity> implements FormDeploymentDataManager {

    public MybatisFormDeploymentDataManager(FormEngineConfiguration formEngineConfiguration) {
        super(formEngineConfiguration);
    }

    @Override
    public Class<? extends FormDeploymentEntity> getManagedEntityClass() {
        return FormDeploymentEntityImpl.class;
    }

    @Override
    public FormDeploymentEntity create() {
        return new FormDeploymentEntityImpl();
    }

    @Override
    public long findDeploymentCountByQueryCriteria(FormDeploymentQueryImpl deploymentQuery) {
        return (Long) getDbSqlSession().selectOne("selectFormDeploymentCountByQueryCriteria", deploymentQuery);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FormDeployment> findDeploymentsByQueryCriteria(FormDeploymentQueryImpl deploymentQuery) {
        return getDbSqlSession().selectList("selectFormDeploymentsByQueryCriteria", deploymentQuery);
    }

    @Override
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return getDbSqlSession().getSqlSession().selectList("selectFormResourceNamesByDeploymentId", deploymentId);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FormDeployment> findDeploymentsByNativeQuery(Map<String, Object> parameterMap) {
        return getDbSqlSession().selectListWithRawParameter("selectFormDeploymentByNativeQuery", parameterMap);
    }

    @Override
    public long findDeploymentCountByNativeQuery(Map<String, Object> parameterMap) {
        return (Long) getDbSqlSession().selectOne("selectFormDeploymentCountByNativeQuery", parameterMap);
    }

}
