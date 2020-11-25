package org.flowable.cmmn.engine.impl.variable;

import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Filip Hrisafov
 */
public class CmmnAggregatedVariableType implements VariableType {

    public static final String TYPE_NAME = "cmmnAggregation";

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
    public void setValue(Object value, ValueFields valueFields) {
        if (value instanceof CmmnAggregation) {
            valueFields.setTextValue(((CmmnAggregation) value).getPlanItemInstanceId());
        } else {
            valueFields.setTextValue(null);
        }
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        return CmmnAggregation.aggregateOverview(valueFields.getTextValue(), valueFields.getName());
    }

}
