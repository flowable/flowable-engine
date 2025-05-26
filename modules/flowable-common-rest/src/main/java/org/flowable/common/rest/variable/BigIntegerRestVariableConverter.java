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

package org.flowable.common.rest.variable;

import java.math.BigInteger;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;

public class BigIntegerRestVariableConverter implements RestVariableConverter {

    @Override
    public String getRestTypeName() {
        return "bigInteger";
    }

    @Override
    public Class<?> getVariableType() {
        return BigInteger.class;
    }

    @Override
    public Object getVariableValue(EngineRestVariable result) {
        if (result.getValue() != null) {
            return new BigInteger(result.getValue().toString());
        }
        return null;
    }

    @Override
    public void convertVariableValue(Object variableValue, EngineRestVariable result) {
        if (variableValue != null) {
            if (!(variableValue instanceof BigInteger)) {
                throw new FlowableIllegalArgumentException("Converter can only convert big integer values");
            }
            result.setValue(variableValue.toString());
            
        } else {
            result.setValue(null);
        }
    }

}
