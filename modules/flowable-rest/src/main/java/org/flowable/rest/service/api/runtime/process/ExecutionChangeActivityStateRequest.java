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

import java.util.List;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class ExecutionChangeActivityStateRequest {

    protected List<String> cancelActivityIds;
    protected List<String> startActivityIds;

    public List<String> getCancelActivityIds() {
        return cancelActivityIds;
    }

    @ApiModelProperty(value = "activityIds to be canceled")
    public void setCancelActivityIds(List<String> cancelActivityIds) {
        this.cancelActivityIds = cancelActivityIds;
    }

    public List<String> getStartActivityIds() {
        return startActivityIds;
    }

    @ApiModelProperty(value = "activityIds to be started")
    public void setStartActivityIds(List<String> startActivityIds) {
        this.startActivityIds = startActivityIds;
    }
}
