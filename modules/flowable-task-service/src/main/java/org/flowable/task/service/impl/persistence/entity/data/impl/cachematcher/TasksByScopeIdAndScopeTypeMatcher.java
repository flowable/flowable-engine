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
package org.flowable.task.service.impl.persistence.entity.data.impl.cachematcher;

import java.util.Map;

import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;

/**
 * @author Joram Barrez
 */
public class TasksByScopeIdAndScopeTypeMatcher extends CachedEntityMatcherAdapter<TaskEntity> {

    @SuppressWarnings("unchecked")
    @Override
    public boolean isRetained(TaskEntity taskEntity, Object parameter) {
        String scopeId = ((Map<String, String>) parameter).get("scopeId");
        String scopeType = ((Map<String, String>) parameter).get("scopeType");
        return taskEntity.getScopeId() != null && taskEntity.getScopeId().equals(scopeId)
                && taskEntity.getScopeType() != null && taskEntity.getScopeType().equals(scopeType);
    }

}