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
package org.flowable.cmmn.test.listener;

import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;

/**
 * @author Joram Barrez
 */
public class TestFilterTypesLifeCycleListener extends AbstractTestLifeCycleListener {

    @Override
    public List<String> getItemDefinitionTypes() {
        return Collections.singletonList(PlanItemDefinitionType.HUMAN_TASK);
    }

    @Override
    public String getSourceState() {
        return null;
    }

    @Override
    public String getTargetState() {
        return null;
    }

}
