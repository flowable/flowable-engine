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

package org.flowable.cmmn.rest.service.api.history.milestone;

import org.flowable.common.rest.api.PaginateRequest;

import java.util.Date;

/**
 * @author Dennis Federico
 */
public class HistoricMilestoneInstanceQueryRequest extends PaginateRequest {

    private String id;
    private String name;
    private String caseInstanceId;
    private String caseDefinitionId;
    private Date reachedBefore;
    private Date reachedAfter;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public Date getReachedBefore() {
        return reachedBefore;
    }

    public void setReachedBefore(Date reachedBefore) {
        this.reachedBefore = reachedBefore;
    }

    public Date getReachedAfter() {
        return reachedAfter;
    }

    public void setReachedAfter(Date reachedAfter) {
        this.reachedAfter = reachedAfter;
    }
}
