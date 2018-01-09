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

package org.flowable.rest.service.api.runtime.process;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class ExecutionChangeActivityStateRequest {

    protected String cancelActivityId;
    protected String startActivityId;

    public String getCancelActivityId() {
        return cancelActivityId;
    }

    @ApiModelProperty(value = "activityId to be canceled")
    public void setCancelActivityId(String cancelActivityId) {
        this.cancelActivityId = cancelActivityId;
    }

    public String getStartActivityId() {
        return startActivityId;
    }

    @ApiModelProperty(value = "activityId to be started")
    public void setStartActivityId(String startActivityId) {
        this.startActivityId = startActivityId;
    }
}
