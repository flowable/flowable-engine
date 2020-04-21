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
package org.flowable.validation;

public class ValidationError {

    protected String validatorSetName;

    protected String problem;

    // Default description in english.
    // Other languages can map the validatorSetName/validatorName to the
    // translated version.
    protected String defaultDescription;

    protected String processDefinitionId;

    protected String processDefinitionName;

    protected int xmlLineNumber;

    protected int xmlColumnNumber;

    protected String activityId;

    protected String activityName;

    protected boolean isWarning;

    public String getValidatorSetName() {
        return validatorSetName;
    }

    public void setValidatorSetName(String validatorSetName) {
        this.validatorSetName = validatorSetName;
    }

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem;
    }

    public String getDefaultDescription() {
        return defaultDescription;
    }

    public void setDefaultDescription(String defaultDescription) {
        this.defaultDescription = defaultDescription;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    public int getXmlLineNumber() {
        return xmlLineNumber;
    }

    public void setXmlLineNumber(int xmlLineNumber) {
        this.xmlLineNumber = xmlLineNumber;
    }

    public int getXmlColumnNumber() {
        return xmlColumnNumber;
    }

    public void setXmlColumnNumber(int xmlColumnNumber) {
        this.xmlColumnNumber = xmlColumnNumber;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public boolean isWarning() {
        return isWarning;
    }

    public void setWarning(boolean isWarning) {
        this.isWarning = isWarning;
    }

    @Override
    public String toString() {
        StringBuilder strb = new StringBuilder();
        strb.append("[Validation set: '").append(validatorSetName).append("' | Problem: '").append(problem).append("'] : ");
        strb.append(defaultDescription);
        strb.append(" - [Extra info : ");
        boolean extraInfoAlreadyPresent = false;
        if (processDefinitionId != null) {
            strb.append("processDefinitionId = ").append(processDefinitionId);
            extraInfoAlreadyPresent = true;
        }
        if (processDefinitionName != null) {
            if (extraInfoAlreadyPresent) {
                strb.append(" | ");
            }
            strb.append("processDefinitionName = ").append(processDefinitionName).append(" | ");
            extraInfoAlreadyPresent = true;
        }
        if (activityId != null) {
            if (extraInfoAlreadyPresent) {
                strb.append(" | ");
            }
            strb.append("id = ").append(activityId).append(" | ");
            extraInfoAlreadyPresent = true;
        }
        if (activityName != null) {
            if (extraInfoAlreadyPresent) {
                strb.append(" | ");
            }
            strb.append("activityName = ").append(activityName).append(" | ");
            extraInfoAlreadyPresent = true;
        }
        strb.append("]");
        if (xmlLineNumber > 0 && xmlColumnNumber > 0) {
            strb.append(" ( line: ").append(xmlLineNumber).append(", column: ").append(xmlColumnNumber).append(")");
        }
        return strb.toString();
    }

}
