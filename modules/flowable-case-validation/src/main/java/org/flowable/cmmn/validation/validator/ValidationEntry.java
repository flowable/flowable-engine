/*
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 *
 */

package org.flowable.cmmn.validation.validator;

/**
 * @author Calin Cerchez
 */
public class ValidationEntry {

    public enum Level {
        Warning, Error
    }

    protected String validatorSetName;

    protected Level level = Level.Error;
    protected String caseDefinitionId;
    protected String caseDefinitionName;
    protected int xmlLineNumber;
    protected int xmlColumnNumber;
    protected String problem;
    // Default description in english.
    // Other languages can map the validatorSetName/validatorName to the
    // translated version.
    protected String defaultDescription;
    protected String itemId;
    protected String itemName;

    public String getValidatorSetName() {
        return validatorSetName;
    }

    public void setValidatorSetName(String validatorSetName) {
        this.validatorSetName = validatorSetName;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public String getCaseDefinitionName() {
        return caseDefinitionName;
    }

    public void setCaseDefinitionName(String caseDefinitionName) {
        this.caseDefinitionName = caseDefinitionName;
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

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    @Override
    public String toString() {
        StringBuilder strb = new StringBuilder();
        strb.append("[Validation set: '").append(validatorSetName).append("' | Problem: '").append(problem).append("'] : ");
        strb.append(defaultDescription);
        strb.append(" - [Extra info : ");
        boolean extraInfoAlreadyPresent = false;
        if (caseDefinitionId != null) {
            strb.append("caseDefinitionId = ").append(caseDefinitionId);
            extraInfoAlreadyPresent = true;
        }
        if (caseDefinitionName != null) {
            if (extraInfoAlreadyPresent) {
                strb.append(" | ");
            }
            strb.append("caseDefinitionName = ").append(caseDefinitionName).append(" | ");
            extraInfoAlreadyPresent = true;
        }
        if (itemId != null) {
            if (extraInfoAlreadyPresent) {
                strb.append(" | ");
            }
            strb.append("id = ").append(itemId).append(" | ");
            extraInfoAlreadyPresent = true;
        }
        if (itemName != null) {
            if (extraInfoAlreadyPresent) {
                strb.append(" | ");
            }
            strb.append("name = ").append(itemName).append(" | ");
            extraInfoAlreadyPresent = true;
        }
        strb.append("]");
        if (xmlLineNumber > 0 && xmlColumnNumber > 0) {
            strb.append(" ( line: ").append(xmlLineNumber).append(", column: ").append(xmlColumnNumber).append(")");
        }
        return strb.toString();
    }

}
