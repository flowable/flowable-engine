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
package org.flowable.cmmn.validation;

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.validation.validator.ValidationEntry;
import org.flowable.cmmn.validation.validator.ValidatorSet;

/**
 * @author Filip Hrisafov
 */
public class CaseValidationContextImpl implements CaseValidationContext {

    protected final ValidatorSet validatorSet;
    protected final List<ValidationEntry> entries = new ArrayList<>();

    public CaseValidationContextImpl(ValidatorSet validatorSet) {
        this.validatorSet = validatorSet;
    }

    @Override
    public ValidationEntry addEntry(ValidationEntry entry) {
        entry.setValidatorSetName(validatorSet.getName());
        entries.add(entry);
        return entry;
    }

    public List<ValidationEntry> getEntries() {
        return entries;
    }
}
