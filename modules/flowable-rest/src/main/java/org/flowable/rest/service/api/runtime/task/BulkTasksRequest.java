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

package org.flowable.rest.service.api.runtime.task;

import java.util.Collection;

/**
 * Request body containing a bulk of tasks and general properties.
 *
 * @author Christopher Welsch
 */
public class BulkTasksRequest extends TaskRequest {

    private Collection<String> taskIds;

    public Collection<String> getTaskIds() {
        return taskIds;
    }

    public void setTaskIds(Collection<String> taskIds) {
        this.taskIds = taskIds;
    }
}
