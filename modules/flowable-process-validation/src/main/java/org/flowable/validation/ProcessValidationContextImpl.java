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

import java.util.ArrayList;
import java.util.List;

import org.flowable.validation.validator.ValidatorSet;

/**
 * @author Tijs Rademakers
 */
public class ProcessValidationContextImpl implements ProcessValidationContext {

    protected final ValidatorSet validatorSet;
    protected final List<ValidationError> entries = new ArrayList<>();

    public ProcessValidationContextImpl(ValidatorSet validatorSet) {
        this.validatorSet = validatorSet;
    }

    @Override
    public ValidationError addEntry(ValidationError entry) {
        entry.setValidatorSetName(validatorSet.getName());
        entries.add(entry);
        return entry;
    }

    public List<ValidationError> getEntries() {
        return entries;
    }
}
