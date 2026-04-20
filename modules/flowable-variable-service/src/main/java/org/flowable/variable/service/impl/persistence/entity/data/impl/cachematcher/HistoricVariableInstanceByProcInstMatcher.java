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
package org.flowable.variable.service.impl.persistence.entity.data.impl.cachematcher;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class HistoricVariableInstanceByProcInstMatcher extends CachedEntityMatcherAdapter<HistoricVariableInstanceEntity> {

    @Override
    public boolean isRetained(HistoricVariableInstanceEntity historicVariableInstanceEntity, Object parameter) {
        Map<String, Object> parameters = (Map<String, Object>) parameter;
        if (!Objects.equals(parameters.get("processInstanceId"), historicVariableInstanceEntity.getProcessInstanceId())) {
            return false;
        }
        Collection<String> variableNames = (Collection<String>) parameters.get("variableNames");
        if (variableNames != null && !variableNames.isEmpty()) {
            return variableNames.contains(historicVariableInstanceEntity.getName());
        }
        return false;
    }

}