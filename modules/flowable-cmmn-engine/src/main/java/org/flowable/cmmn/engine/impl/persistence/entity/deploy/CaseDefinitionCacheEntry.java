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
package org.flowable.cmmn.engine.impl.persistence.entity.deploy;

import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;

public class CaseDefinitionCacheEntry {
    
    protected CaseDefinition caseDefinition;
    protected CmmnModel cmmnModel;
    protected Case caze;

    public CaseDefinitionCacheEntry(CaseDefinition caseDefinition, CmmnModel cmmnModel, Case caze) {
        this.caseDefinition = caseDefinition;
        this.cmmnModel = cmmnModel;
        this.caze = caze;
    }

    public CaseDefinition getCaseDefinition() {
        return caseDefinition;
    }

    public void setCaseDefinition(CaseDefinition caseDefinition) {
        this.caseDefinition = caseDefinition;
    }

    public CmmnModel getCmmnModel() {
        return cmmnModel;
    }

    public void setCmmnModel(CmmnModel cmmnModel) {
        this.cmmnModel = cmmnModel;
    }

    public Case getCase() {
        return caze;
    }

    public void setCase(Case caze) {
        this.caze = caze;
    }

}
