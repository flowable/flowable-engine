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
package org.flowable.cmmn.model;

/**
 * @author Joram Barrez
 */
public class CaseFileItem extends AbstractCaseFileItemContainer {

    public static enum CaseFileItemMultiplicity {
      ZERO_OR_ONE, ZERO_OR_MORE, EXACTLY_ONE, ONE_OR_MORE, UNSPECIFIED, UNKNOWN
    };

    protected String name;
    protected CaseFileItemMultiplicity multiplicity =  CaseFileItemMultiplicity.UNSPECIFIED;

    protected String caseFileItemDefinitionRef;
    protected CaseFileItemDefinition caseFileItemDefinition;

    /*
     * The spec has a 'children' element which is meant for nested child items.
     * However, there is no parent element in the xsd (although the pdf does mention it).
     * (This is probably a bug in the spec)
     *
     * Furthermore, source/target refs don't add metadata which is important
     * when wanting to associate content with each other.
     *
     * So, we parse source and target refs into child/parent
     * and don't expose the source/target refs directly.
     */

    protected String sourceCaseFileItemRef;
    protected String targetCaseFileItemRefs;

    protected CaseFileItem parentCaseFileItem;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public CaseFileItemMultiplicity getMultiplicity() {
        return multiplicity;
    }
    public void setMultiplicity(CaseFileItemMultiplicity multiplicity) {
        this.multiplicity = multiplicity;
    }
    public String getCaseFileItemDefinitionRef() {
        return caseFileItemDefinitionRef;
    }
    public void setCaseFileItemDefinitionRef(String caseFileItemDefinitionRef) {
        this.caseFileItemDefinitionRef = caseFileItemDefinitionRef;
    }
    public CaseFileItemDefinition getCaseFileItemDefinition() {
        return caseFileItemDefinition;
    }
    public void setCaseFileItemDefinition(CaseFileItemDefinition caseFileItemDefinition) {
        this.caseFileItemDefinition = caseFileItemDefinition;
    }
    public String getSourceCaseFileItemRef() {
        return sourceCaseFileItemRef;
    }
    public void setSourceCaseFileItemRef(String sourceCaseFileItemRef) {
        this.sourceCaseFileItemRef = sourceCaseFileItemRef;
    }
    public String getTargetCaseFileItemRefs() {
        return targetCaseFileItemRefs;
    }
    public void setTargetCaseFileItemRefs(String targetCaseFileItemRefs) {
        this.targetCaseFileItemRefs = targetCaseFileItemRefs;
    }
    public CaseFileItem getParentCaseFileItem() {
        return parentCaseFileItem;
    }
    public void setParentCaseFileItem(CaseFileItem parentCaseFileItem) {
        this.parentCaseFileItem = parentCaseFileItem;
    }
}
