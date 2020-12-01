package org.flowable.engine.impl.variable;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.variable.api.types.ValueFields;
import org.flowable.variable.api.types.VariableType;

/**
 * @author Filip Hrisafov
 */
public class BpmnAggregatedVariableType implements VariableType {

    public static final String TYPE_NAME = "bpmnAggregation";

    protected final ProcessEngineConfigurationImpl processEngineConfiguration;

    public BpmnAggregatedVariableType(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
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
        return value instanceof BpmnAggregation;
    }

    @Override
    public boolean isReadOnly() {
        return true;
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
        CommandContext commandContext = Context.getCommandContext();
        if (commandContext != null) {
            return BpmnAggregation.aggregateOverview(valueFields.getTextValue(), valueFields.getName(), commandContext);
        } else {
            return processEngineConfiguration.getCommandExecutor()
                    .execute(context -> BpmnAggregation.aggregateOverview(valueFields.getTextValue(), valueFields.getName(), context));
        }
    }

}
