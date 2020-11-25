package org.flowable.engine.impl.variable;

import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Filip Hrisafov
 */
public class BpmnAggregatedVariableType implements VariableType {

    public static final String TYPE_NAME = "bpmnAggregation";

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
        return value instanceof BpmnAggregation;
    }

    @Override
    public void setValue(Object value, ValueFields valueFields) {
        if (value instanceof BpmnAggregation) {
            valueFields.setTextValue(((BpmnAggregation) value).getExecutionId());
        } else {
            valueFields.setTextValue(null);
        }
    }

    @Override
    public Object getValue(ValueFields valueFields) {
        return BpmnAggregation.aggregateOverview(valueFields.getTextValue(), valueFields.getName());
    }

}
