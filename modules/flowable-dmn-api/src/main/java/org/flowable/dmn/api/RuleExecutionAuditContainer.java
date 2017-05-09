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
package org.flowable.dmn.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class RuleExecutionAuditContainer {

    protected Date startTime;
    protected Date endTime;

    protected int ruleNumber;
    protected Boolean valid = Boolean.FALSE;

    @JsonProperty("exception")
    protected String exceptionMessage;

    protected Map<Integer, ExpressionExecution> conditionResults = new HashMap<>();
    protected Map<Integer, ExpressionExecution> conclusionResults = new HashMap<>();

    public RuleExecutionAuditContainer(int ruleNumber) {
        this.ruleNumber = ruleNumber;
        this.startTime = new Date();
    }

    public void addConditionResult(int inputNumber, ExpressionExecution expressionExecution) {
        conditionResults.put(inputNumber, expressionExecution);
    }

    public void addConclusionResult(int outputNumber, ExpressionExecution executionResult) {
        conclusionResults.put(outputNumber, executionResult);
    }

    public void markRuleEnd() {
        endTime = new Date();
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public int getRuleNumber() {
        return ruleNumber;
    }

    public Boolean isValid() {
        return valid;
    }

    public void setValid() {
        this.valid = Boolean.TRUE;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public Map<Integer, ExpressionExecution> getConditionResults() {
        return conditionResults;
    }

    public Map<Integer, ExpressionExecution> getConclusionResults() {
        return conclusionResults;
    }

}
