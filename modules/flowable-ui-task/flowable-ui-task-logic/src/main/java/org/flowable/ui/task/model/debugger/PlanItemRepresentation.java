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

/**
 * @author martin.grofcik
 */
public class PlanItemRepresentation {

    protected String id;
    protected String caseInstanceId;
    protected String stageInstanceId;
    protected String elementId;
    protected Boolean isCompleteable;
    protected String tenantId;

    public PlanItemRepresentation(String id, String caseInstanceId, String stageInstanceId,
                                   String elementId, boolean isCompleteable, String tenantId) {
        this.id = id;
        this.caseInstanceId = caseInstanceId;
        this.stageInstanceId = stageInstanceId;
        this.elementId = elementId;
        this.isCompleteable = isCompleteable;
        this.tenantId = tenantId;
    }

    public String getId() {
        return id;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }
    public String getStageInstanceId() {
        return stageInstanceId;
    }
    public String getElementId() {
        return elementId;
    }
    public Boolean getCompleteable() {
        return isCompleteable;
    }
    public String getTenantId() {
        return tenantId;
    }
}
