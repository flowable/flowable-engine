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
