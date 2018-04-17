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

import java.util.Map;

import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class HistoricVariableInstanceByScopeIdAndScopeTypeMatcher extends CachedEntityMatcherAdapter<HistoricVariableInstanceEntity> {

    @Override
    public boolean isRetained(HistoricVariableInstanceEntity historicVariableInstanceEntity, Object parameter) {
        Map<String, String> map = (Map<String, String>) parameter;
        return map.get("scopeId").equals(historicVariableInstanceEntity.getScopeId())
                && map.get("scopeType").equals(historicVariableInstanceEntity.getScopeType());
    }

}