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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Joram Barrez
 */
public abstract class AbstractCaseFileItemContainer extends CaseElement implements CaseFileItemContainer {

    protected List<CaseFileItem> caseFileItems = new ArrayList<>();

    public CaseFileItem findFileItem(String itemId) {
        for (CaseFileItem caseFileItem : caseFileItems) {
            if (Objects.equals(itemId, caseFileItem.getId())) {
                return caseFileItem;
            }

            if (!caseFileItem.getCaseFileItems().isEmpty()) {
                CaseFileItem fileItem = caseFileItem.findFileItem(itemId);
                if (fileItem != null) {
                    return fileItem;
                }
            }
        }
        return null;
    }

    public void addCaseFileItem(CaseFileItem caseFileItem) {
        this.caseFileItems.add(caseFileItem);
    }

    public List<CaseFileItem> getCaseFileItems() {
        return caseFileItems;
    }

    public void setCaseFileItems(List<CaseFileItem> caseFileItems) {
        this.caseFileItems = caseFileItems;
    }

}
