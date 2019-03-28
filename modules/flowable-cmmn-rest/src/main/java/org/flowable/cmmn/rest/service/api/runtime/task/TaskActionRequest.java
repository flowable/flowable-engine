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

package org.flowable.cmmn.rest.service.api.runtime.task;

import java.util.List;

import org.flowable.cmmn.rest.service.api.RestActionRequest;
import org.flowable.cmmn.rest.service.api.engine.variable.RestVariable;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Frederik Heremans
 */
public class TaskActionRequest extends RestActionRequest {

    public static final String ACTION_COMPLETE = "complete";
    public static final String ACTION_CLAIM = "claim";
    public static final String ACTION_DELEGATE = "delegate";
    public static final String ACTION_RESOLVE = "resolve";

    protected String assignee;
    protected String formDefinitionId;
    protected String outcome;
    protected List<RestVariable> variables;
    protected List<RestVariable> transientVariables;

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    @ApiModelProperty(value = "If action is claim or delegate, you can use this parameter to set the assignee associated ", example = "userWhoClaims/userToDelegateTo")
    public String getAssignee() {
        return assignee;
    }
    
    @ApiModelProperty(value = "Required when completing a task with a form", example = "12345")
    public String getFormDefinitionId() {
        return formDefinitionId;
    }

    public void setFormDefinitionId(String formDefinitionId) {
        this.formDefinitionId = formDefinitionId;
    }

    @ApiModelProperty(value = "Optional outcome value when completing a task with a form", example = "accepted/rejected")
    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public void setVariables(List<RestVariable> variables) {
        this.variables = variables;
    }

    @ApiModelProperty(value = "If action is complete, you can use this parameter to set variables ")
    @JsonTypeInfo(use = Id.CLASS, defaultImpl = RestVariable.class)
    public List<RestVariable> getVariables() {
        return variables;
    }

    @ApiModelProperty(value = "If action is complete, you can use this parameter to set transient variables ")
    public List<RestVariable> getTransientVariables() {
        return transientVariables;
    }

    @JsonTypeInfo(use = Id.CLASS, defaultImpl = RestVariable.class)
    public void setTransientVariables(List<RestVariable> transientVariables) {
        this.transientVariables = transientVariables;
    }

    @Override
    @ApiModelProperty(value = "Action to perform: Either complete, claim, delegate or resolve", example = "complete", required = true)
    public String getAction() {
        return super.getAction();
    }

}
