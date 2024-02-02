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
package org.flowable.engine.impl.persistence.entity.data.impl.cachematcher;

import java.util.Map;
import java.util.Objects;

import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntity;

/**
 * @author Joram Barrez
 */
public class UnfinishedHistoricActivityInstanceByProcessInstanceIdMatcher extends CachedEntityMatcherAdapter<HistoricActivityInstanceEntity> {

    @Override
    public boolean isRetained(HistoricActivityInstanceEntity entity, Object parameter) {
        Map<String, String> paramMap = (Map<String, String>) parameter;
        String processInstanceId = paramMap.get("processInstanceId");

        return Objects.equals(processInstanceId, entity.getProcessInstanceId()) && entity.getEndTime() == null;
    }

}