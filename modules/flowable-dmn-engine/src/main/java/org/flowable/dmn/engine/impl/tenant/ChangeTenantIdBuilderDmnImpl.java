package org.flowable.dmn.engine.impl.tenant;

import org.flowable.dmn.engine.impl.cmd.ChangeTenantIdDmnCompleteCmd;
import org.flowable.dmn.engine.impl.cmd.ChangeTenantIdDmnSimulateCmd;
import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

public class ChangeTenantIdBuilderDmnImpl implements ChangeTenantIdBuilder {

    private final CommandExecutor commandExecutor;
    private final String fromTenantId;
    private final String toTenantId;
    private boolean onlyInstancesFromDefaultTenantDefinitions;

    public ChangeTenantIdBuilderDmnImpl(CommandExecutor commandExecutor, String fromTenantId,
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
        return commandExecutor.execute(new ChangeTenantIdDmnSimulateCmd(fromTenantId, toTenantId,
                onlyInstancesFromDefaultTenantDefinitions));
    }

    @Override
    public ChangeTenantIdResult complete() {
        return commandExecutor.execute(new ChangeTenantIdDmnCompleteCmd(fromTenantId, toTenantId,
                onlyInstancesFromDefaultTenantDefinitions));
    }

}