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
package org.flowable.identitylink.service.impl.persistence.entity.data.impl.cachematcher;

import java.util.Map;
import java.util.Objects;

import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcherAdapter;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;

/**
 * @author Filip Hrisafov
 */
public class IdentityLinksByProcessInstanceUserGroupAndTypeMatcher extends CachedEntityMatcherAdapter<IdentityLinkEntity> {

    @Override
    public boolean isRetained(IdentityLinkEntity entity, Object parameter) {
        @SuppressWarnings("unchecked")
        Map<String, String> parameterMap = (Map<String, String>) parameter;
        if (!Objects.equals(entity.getProcessInstanceId(), parameterMap.get("processInstanceId"))) {
            return false;
        }
        if (!Objects.equals(entity.getUserId(), parameterMap.get("userId"))) {
            return false;
        }
        if (!Objects.equals(entity.getGroupId(), parameterMap.get("groupId"))) {
            return false;
        }
        return Objects.equals(entity.getType(), parameterMap.get("type"));
    }

}