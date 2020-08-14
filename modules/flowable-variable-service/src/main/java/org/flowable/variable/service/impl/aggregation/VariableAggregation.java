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
package org.flowable.variable.service.impl.aggregation;

/**
 * @author Joram Barrez
 */
public class VariableAggregation {

    protected String targetArrayVariable;
    protected String source;
    protected String target;

    public VariableAggregation(String targetArrayVariable, String source, String target) {
        this.targetArrayVariable = targetArrayVariable;
        this.source = source;
        this.target = target;
    }

    public String getTargetArrayVariable() {
        return targetArrayVariable;
    }

    public void setTargetArrayVariable(String targetArrayVariable) {
        this.targetArrayVariable = targetArrayVariable;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

}
