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
package org.flowable.cmmn.engine.impl.persistence.entity.data.impl.matcher;

import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;

/**
 * @author Joram Barrez
 */
public class CaseInstanceByCaseDefinitionIdMatcher extends CachedEntityMatcherAdapter<CaseInstanceEntity> {

    public boolean isRetained(CaseInstanceEntity entity, Object param) {
        return entity.getCaseDefinitionId().equals(param);
    }
    
}
