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
package org.flowable.ui.task.model.runtime;

import java.util.Date;

import org.flowable.cmmn.api.runtime.MilestoneInstance;
import org.flowable.ui.common.model.AbstractRepresentation;

public class MilestoneRepresentation extends AbstractRepresentation {

    protected String name;
    protected String definitionId;
    protected Date timestamp;

    public MilestoneRepresentation(String name, String definitionId, Date timestamp) {
        this.name = name;
        this.definitionId = definitionId;
        this.timestamp = timestamp;
    }

    public MilestoneRepresentation(MilestoneInstance milestoneInstance) {
        this (milestoneInstance.getName(), milestoneInstance.getElementId(), milestoneInstance.getTimeStamp());
    }

    public String getName() {
        return name;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
