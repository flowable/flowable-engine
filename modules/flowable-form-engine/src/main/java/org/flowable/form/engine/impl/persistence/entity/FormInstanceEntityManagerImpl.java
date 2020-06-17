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

import org.flowable.common.engine.impl.persistence.entity.AbstractEngineEntityManager;
import org.flowable.form.api.FormInstance;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.FormInstanceQueryImpl;
import org.flowable.form.engine.impl.persistence.entity.data.FormInstanceDataManager;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class FormInstanceEntityManagerImpl
    extends AbstractEngineEntityManager<FormEngineConfiguration, FormInstanceEntity, FormInstanceDataManager>
    implements FormInstanceEntityManager {

    public FormInstanceEntityManagerImpl(FormEngineConfiguration formEngineConfiguration, FormInstanceDataManager formInstanceDataManager) {
        super(formEngineConfiguration, formInstanceDataManager);
    }

    @Override
    public long findFormInstanceCountByQueryCriteria(FormInstanceQueryImpl formInstanceQuery) {
        return dataManager.findFormInstanceCountByQueryCriteria(formInstanceQuery);
    }

    @Override
    public List<FormInstance> findFormInstancesByQueryCriteria(FormInstanceQueryImpl formInstanceQuery) {
        return dataManager.findFormInstancesByQueryCriteria(formInstanceQuery);
    }
    
    @Override
    public void deleteFormInstancesByFormDefinitionId(String formDefinitionId) {
        dataManager.deleteFormInstancesByFormDefinitionId(formDefinitionId);
        // The form instance values are persisted as bytes in the Form Resource with a name having form-<formDefinitionId>
        // Have a look at FormInstanceEntityImpl#setFormValueBytes
        getResourceEntityManager().deleteResourcesByName("form-" + formDefinitionId);
    }
    
    @Override
    public void deleteFormInstancesByProcessDefinitionId(String processDefinitionId) {
        dataManager.deleteFormInstancesByProcessDefinitionId(processDefinitionId);
    }

    @Override
    public void deleteFormInstancesByScopeDefinitionId(String scopeDefinitionId) {
        dataManager.deleteFormInstancesByScopeDefinitionId(scopeDefinitionId);
    }

    protected FormResourceEntityManager getResourceEntityManager() {
        return engineConfiguration.getResourceEntityManager();
    }
}
