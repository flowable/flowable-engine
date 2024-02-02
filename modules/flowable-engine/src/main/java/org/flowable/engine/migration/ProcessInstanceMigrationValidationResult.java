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

package org.flowable.engine.migration;

import java.util.ArrayList;
import java.util.List;

public class ProcessInstanceMigrationValidationResult {

    protected List<String> validationMessages = new ArrayList<>();

    public ProcessInstanceMigrationValidationResult addValidationMessage(String message) {
        validationMessages.add(message);
        return this;
    }

    public ProcessInstanceMigrationValidationResult addValidationResult(ProcessInstanceMigrationValidationResult result) {
        if (result != null) {
            validationMessages.addAll(result.validationMessages);
        }
        return this;
    }

    public boolean hasErrors() {
        return !validationMessages.isEmpty();
    }

    public boolean isMigrationValid() {
        return validationMessages.isEmpty();
    }

    public List<String> getValidationMessages() {
        return validationMessages;
    }
}
