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

import java.util.Collection;
import java.util.Objects;

import org.flowable.common.engine.impl.persistence.cache.CachedEntity;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntity;

/**
 * author martin.grofcik
 */
public class ActivityByProcessInstanceIdMatcher implements CachedEntityMatcher<ActivityInstanceEntity> {

    @Override
    public boolean isRetained(Collection<ActivityInstanceEntity> databaseEntities, Collection<CachedEntity> cachedEntities, 
                    ActivityInstanceEntity entity, Object param) {
        
        String processInstanceId = (String) param;
        return Objects.equals(entity.getProcessInstanceId(), processInstanceId);
    }
}
