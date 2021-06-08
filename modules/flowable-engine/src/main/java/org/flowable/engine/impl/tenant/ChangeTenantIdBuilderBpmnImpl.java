package org.flowable.engine.impl.tenant;

import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.cmd.ChangeTenantIdBpmnCompleteCmd;
import org.flowable.engine.impl.cmd.ChangeTenantIdBpmnSimulateCmd;

public class ChangeTenantIdBuilderBpmnImpl implements ChangeTenantIdBuilder {

    private final CommandExecutor commandExecutor;
    private final String fromTenantId;
    private final String toTenantId;
    private boolean onlyInstancesFromDefaultTenantDefinitions;

    public ChangeTenantIdBuilderBpmnImpl(CommandExecutor commandExecutor, String fromTenantId,
            String toTenantId) {
        this.commandExecutor = commandExecutor;
        this.fromTenantId = fromTenantId;
        this.toTenantId = toTenantId;
    }

    @Override
    public ChangeTenantIdBuilder onlyInstancesFromDefaultTenantDefinitions(
            boolean onlyInstancesFromDefaultTenantDefinitionsEnabled) {
        this.onlyInstancesFromDefaultTenantDefinitions = onlyInstancesFromDefaultTenantDefinitionsEnabled;
        return this;
    }

    @Override
    public ChangeTenantIdResult simulate() {
        return commandExecutor.execute(new ChangeTenantIdBpmnSimulateCmd(fromTenantId, toTenantId,
                onlyInstancesFromDefaultTenantDefinitions));
    }

    @Override
    public ChangeTenantIdResult complete() {
        return commandExecutor.execute(new ChangeTenantIdBpmnCompleteCmd(fromTenantId, toTenantId,
                onlyInstancesFromDefaultTenantDefinitions));
    }

}
