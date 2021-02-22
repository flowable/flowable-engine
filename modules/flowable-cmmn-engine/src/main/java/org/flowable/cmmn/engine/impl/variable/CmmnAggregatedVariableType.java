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
package org.flowable.cmmn.engine.impl.variable;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Filip Hrisafov
 */
public class CmmnAggregatedVariableType implements VariableType {

    public static final String TYPE_NAME = "cmmnAggregation";

    protected final CmmnEngineConfiguration cmmnEngineConfiguration;

    public CmmnAggregatedVariableType(CmmnEngineConfiguration cmmnEngineConfiguration) {
        this.cmmnEngineConfiguration = cmmnEngineConfiguration;
    }

    @Override
    public String getTypeName() {
        return TYPE_NAME;
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public boolean isAbleToStore(Object value) {
        return value instanceof CmmnAggregation;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        if (value instanceof CmmnAggregation) {
            valueFields.setTextValue(((CmmnAggregation) value).getPlanItemInstanceId());
        } else {
            valueFields.setTextValue(null);
        }
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            return CmmnAggregation.aggregateOverview(valueFields.getTextValue(), valueFields.getName(), commandContext);
        } else {
            return cmmnEngineConfiguration.getCommandExecutor()
                    .execute(context -> CmmnAggregation.aggregateOverview(valueFields.getTextValue(), valueFields.getName(), context));
        }
    }

}
