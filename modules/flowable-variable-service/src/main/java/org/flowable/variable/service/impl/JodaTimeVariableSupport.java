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
package org.flowable.variable.service.impl;

import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.impl.types.JodaDateFallbackType;
import org.flowable.variable.service.impl.types.JodaDateTimeFallbackType;
import org.flowable.variable.service.impl.types.JodaDateTimeType;
import org.flowable.variable.service.impl.types.JodaDateType;
import org.springframework.util.ClassUtils;

/**
 * @author Filip Hrisafov
 */
public enum JodaTimeVariableSupport {

    DISABLE {
        @Override
        public void registerJodaTypes(VariableTypes variableTypes) {
            // Nothing to do
        }
    },
    READ_AS_JAVA_TIME {
        @Override
        public void registerJodaTypes(VariableTypes variableTypes) {
            variableTypes.addType(new JodaDateFallbackType());
            variableTypes.addType(new JodaDateTimeFallbackType());
        }
    },
    @Deprecated
    WRITE {
        @Override
        public void registerJodaTypes(VariableTypes variableTypes) {
            if (ClassUtils.isPresent("org.joda.time.DateTime", null)) {
                variableTypes.addType(new JodaDateType());
                variableTypes.addType(new JodaDateTimeType());
            } else {
                throw new FlowableIllegalStateException("Cannot use write JodaTimeVariable support when joda-time is not present");
            }
        }
    },
    ;

    public abstract void registerJodaTypes(VariableTypes variableTypes);
}
