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

package org.flowable.cmmn.rest.service.api.runtime.caze;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class ChangePlanItemStateRequest {

    protected List<String> activatePlanItemDefinitionIds;
    protected List<String> moveToAvailablePlanItemDefinitionIds;
    protected List<String> terminatePlanItemDefinitionIds;

    public List<String> getActivatePlanItemDefinitionIds() {
        return activatePlanItemDefinitionIds;
    }

    @ApiModelProperty(value = "plan item definition ids to be activated")
    public void setActivatePlanItemDefinitionIds(List<String> activatePlanItemDefinitionIds) {
        this.activatePlanItemDefinitionIds = activatePlanItemDefinitionIds;
    }

    public List<String> getMoveToAvailablePlanItemDefinitionIds() {
        return moveToAvailablePlanItemDefinitionIds;
    }

    @ApiModelProperty(value = "plan item definition ids to be moved to available state")
    public void setMoveToAvailablePlanItemDefinitionIds(List<String> moveToAvailablePlanItemDefinitionIds) {
        this.moveToAvailablePlanItemDefinitionIds = moveToAvailablePlanItemDefinitionIds;
    }

    public List<String> getTerminatePlanItemDefinitionIds() {
        return terminatePlanItemDefinitionIds;
    }

    @ApiModelProperty(value = "plan item definition ids to be terminated")
    public void setTerminatePlanItemDefinitionIds(List<String> terminatePlanItemDefinitionIds) {
        this.terminatePlanItemDefinitionIds = terminatePlanItemDefinitionIds;
    }
}
