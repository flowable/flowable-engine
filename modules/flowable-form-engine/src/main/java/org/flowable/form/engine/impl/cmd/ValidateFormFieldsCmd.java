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
package org.flowable.form.engine.impl.cmd;

import java.util.Map;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.form.api.FormInfo;

/**
 * @author martin.grofcik
 */
public class ValidateFormFieldsCmd implements Command<Void> {

    protected FormInfo formInfo;
    protected Map<String, Object> values;

    public ValidateFormFieldsCmd(FormInfo formInfo, Map<String, Object> values) {
        this.formInfo = formInfo;
        this.values = values;
    }

    @Override
    public Void execute(CommandContext commandContext) {
        // empty validation implementation
        return null;
    }

}
