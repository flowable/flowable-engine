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

import java.util.Date;

import org.flowable.cmmn.rest.service.api.RestActionRequest;

/*
 * @author Joram Barrez
 */
public class CaseInstanceUpdateRequest extends RestActionRequest {

    public static final String ACTION_CLAIM = "claim";
    public static final String ACTION_UNCLAIM = "unclaim";

    protected String name;
    protected String businessKey;
    protected String businessStatus;
    protected Date dueDate;
    protected String assignee;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getBusinessKey() {
        return businessKey;
    }
    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }
    public String getBusinessStatus() {
        return businessStatus;
    }
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }
    public Date getDueDate() {
        return dueDate;
    }
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }
    public String getAssignee() {
        return assignee;
    }
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
}
