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
package org.flowable.common.engine.api.variable;

/**
 * @author Arthur Hupka-Merle
 */
class EmptyVariableContainer implements VariableContainer {

    static final EmptyVariableContainer INSTANCE = new EmptyVariableContainer();

    EmptyVariableContainer() {

    }

    @Override
    public boolean hasVariable(String variableName) {
        return false;
    }

    @Override
    public Object getVariable(String variableName) {
        return null;
    }

    @Override
    public void setVariable(String variableName, Object variableValue) {

    }

    @Override
    public void setTransientVariable(String variableName, Object variableValue) {

    }

    @Override
    public String getTenantId() {
        return null;
    }
}
