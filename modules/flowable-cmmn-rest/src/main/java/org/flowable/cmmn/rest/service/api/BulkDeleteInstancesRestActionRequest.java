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
package org.flowable.cmmn.rest.service.api;

import java.util.Collection;

/**
 * Class that represents a bulk action to be performed on a resource.
 *
 * @author Christopher Welsch
 */
public class BulkDeleteInstancesRestActionRequest extends RestActionRequest {

    public static final String DELETE_ACTION = "delete";
    public static final String TERMINATE_ACTION = "terminate";

    protected Collection<String> instanceIds;

    public Collection<String> getInstanceIds() {
        return instanceIds;
    }

    public void setInstanceIds(Collection<String> instanceIds) {
        this.instanceIds = instanceIds;
    }
}

