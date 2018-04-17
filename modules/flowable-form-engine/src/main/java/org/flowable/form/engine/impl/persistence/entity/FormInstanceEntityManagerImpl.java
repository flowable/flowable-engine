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

package org.flowable.form.engine.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.form.api.FormInstance;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormInstanceQueryImpl;
import org.flowable.form.engine.impl.persistence.entity.data.FormInstanceDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FormInstanceEntityManagerImpl extends AbstractEntityManager<FormInstanceEntity> implements FormInstanceEntityManager {

    protected FormInstanceDataManager formInstanceDataManager;

    public FormInstanceEntityManagerImpl(FormEngineConfiguration formEngineConfiguration, FormInstanceDataManager formInstanceDataManager) {
        super(formEngineConfiguration);
        this.formInstanceDataManager = formInstanceDataManager;
    }

    @Override
    public long findFormInstanceCountByQueryCriteria(FormInstanceQueryImpl formInstanceQuery) {
        return formInstanceDataManager.findFormInstanceCountByQueryCriteria(formInstanceQuery);
    }

    @Override
    public List<FormInstance> findFormInstancesByQueryCriteria(FormInstanceQueryImpl formInstanceQuery) {
        return formInstanceDataManager.findFormInstancesByQueryCriteria(formInstanceQuery);
    }

    @Override
    protected DataManager<FormInstanceEntity> getDataManager() {
        return formInstanceDataManager;
    }

    public FormInstanceDataManager getFormInstanceDataManager() {
        return formInstanceDataManager;
    }

    public void setFormInstanceDataManager(FormInstanceDataManager formInstanceDataManager) {
        this.formInstanceDataManager = formInstanceDataManager;
    }

}
