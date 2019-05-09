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
    
    protected String elementId;
    protected String definitionId;

    public BreakpointRepresentation(String definitionId, String elementId) {
        this.definitionId = definitionId;
        this.elementId = elementId;
    }

    public BreakpointRepresentation() {
    }

    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    public void setActivityId(String activityId) {
        this.elementId = activityId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(String definitionId) {
        this.definitionId = definitionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BreakpointRepresentation that = (BreakpointRepresentation) o;

        if (!getElementId().equals(that.getElementId())) {
            return false;
        }
        
        if (definitionId == null && that.getDefinitionId() == null) {
            return true;
        }
        
        return getDefinitionId().equals(that.getDefinitionId());
    }

    @Override
    public int hashCode() {
        int result = getElementId().hashCode();
        result = 31 * result + getDefinitionId().hashCode();
        return result;
    }
}
