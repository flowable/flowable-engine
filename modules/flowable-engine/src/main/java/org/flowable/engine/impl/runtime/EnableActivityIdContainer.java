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
package org.flowable.engine.impl.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Tijs Rademakers
 */
public class EnableActivityIdContainer {

    protected List<String> activityIds;

    public EnableActivityIdContainer(String singleActivityId) {
        this.activityIds = Collections.singletonList(singleActivityId);
    }

    public EnableActivityIdContainer(List<String> activityIds) {
        this.activityIds = activityIds;
    }

    public List<String> getActivityIds() {
        return Optional.ofNullable(activityIds).orElse(Collections.emptyList());
    }
}
