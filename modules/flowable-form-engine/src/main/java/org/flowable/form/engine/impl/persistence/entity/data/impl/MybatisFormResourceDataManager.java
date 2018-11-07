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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.persistence.entity.FormResourceEntity;
import org.flowable.form.engine.impl.persistence.entity.FormResourceEntityImpl;
import org.flowable.form.engine.impl.persistence.entity.data.AbstractFormDataManager;
import org.flowable.form.engine.impl.persistence.entity.data.FormResourceDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisFormResourceDataManager extends AbstractFormDataManager<FormResourceEntity> implements FormResourceDataManager {

    public MybatisFormResourceDataManager(FormEngineConfiguration formEngineConfiguration) {
        super(formEngineConfiguration);
    }

    @Override
    public Class<? extends FormResourceEntity> getManagedEntityClass() {
        return FormResourceEntityImpl.class;
    }

    @Override
    public FormResourceEntity create() {
        return new FormResourceEntityImpl();
    }

    @Override
    public void deleteResourcesByDeploymentId(String deploymentId) {
        getDbSqlSession().delete("deleteFormResourcesByDeploymentId", deploymentId, getManagedEntityClass());
    }

    @Override
    public FormResourceEntity findResourceByDeploymentIdAndResourceName(String deploymentId, String resourceName) {
        Map<String, Object> params = new HashMap<>();
        params.put("deploymentId", deploymentId);
        params.put("resourceName", resourceName);
        return (FormResourceEntity) getDbSqlSession().selectOne("selectFormResourceByDeploymentIdAndResourceName", params);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<FormResourceEntity> findResourcesByDeploymentId(String deploymentId) {
        return getDbSqlSession().selectList("selectFormResourcesByDeploymentId", deploymentId);
    }

}
