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
package org.flowable.ui.task.model.debugger;

import org.flowable.ui.common.model.AbstractRepresentation;

/**
 * REST representation of the breakpoint
 *
 * @author martin.grofcik
 */
public final class BreakpointRepresentation extends AbstractRepresentation {
    
    protected String activityId;
    protected String processDefinitionId;

    public BreakpointRepresentation(String processDefinitionId, String activityId) {
        this.processDefinitionId = processDefinitionId;
        this.activityId = activityId;
    }

    public BreakpointRepresentation() {
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BreakpointRepresentation that = (BreakpointRepresentation) o;

        if (!getActivityId().equals(that.getActivityId())) {
            return false;
        }
        
        if (processDefinitionId == null && that.getProcessDefinitionId() == null) {
            return true;
        }
        
        return getProcessDefinitionId().equals(that.getProcessDefinitionId());
    }

    @Override
    public int hashCode() {
        int result = getActivityId().hashCode();
        result = 31 * result + getProcessDefinitionId().hashCode();
        return result;
    }
}
