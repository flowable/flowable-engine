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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;
import org.flowable.common.rest.util.DateToStringSerializer;

import java.util.Date;

/**
 * @author Dennis Federico
 */
public class HistoricMilestoneInstanceResponse {

    protected String id;
    protected String name;
    protected String elementId;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date timestamp;
    protected String caseInstanceId;
    protected String caseDefinitionId;
    protected String url;
    protected String historicCaseInstanceUrl;
    protected String caseDefinitionUrl;

    @ApiModelProperty(example = "5")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "milestoneName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "milestonePlanItemId")
    public String getElementId() {
        return elementId;
    }

    public void setElementId(String elementId) {
        this.elementId = elementId;
    }

    @ApiModelProperty(example = "2013-04-18T14:06:32.715+0000")
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @ApiModelProperty(example = "12345")
    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    @ApiModelProperty(example = "oneMilestoneCase%3A1%3A4")
    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-history/historic-milestone-instances/5")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-history/historic-case-instances/12345")
    public String getHistoricCaseInstanceUrl() {
        return historicCaseInstanceUrl;
    }

    public void setHistoricCaseInstanceUrl(String historicCaseInstanceUrl) {
        this.historicCaseInstanceUrl = historicCaseInstanceUrl;
    }

    @ApiModelProperty(example = "http://localhost:8182/cmmn-repository/case-definitions/oneMilestoneCase%3A1%3A4")
    public String getCaseDefinitionUrl() {
        return caseDefinitionUrl;
    }

    public void setCaseDefinitionUrl(String caseDefinitionUrl) {
        this.caseDefinitionUrl = caseDefinitionUrl;
    }

}
