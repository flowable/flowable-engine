package org.flowable.cmmn.engine.impl.tenant;

import org.flowable.cmmn.engine.impl.cmd.ChangeTenantIdCmmnCompleteCmd;
import org.flowable.cmmn.engine.impl.cmd.ChangeTenantIdCmmnSimulateCmd;
import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;

public class ChangeTenantIdBuilderCmmnImpl implements ChangeTenantIdBuilder {

    private final CommandExecutor commandExecutor;
    private final String fromTenantId;
    private final String toTenantId;
    private boolean onlyInstancesFromDefaultTenantDefinitions;

    public ChangeTenantIdBuilderCmmnImpl(CommandExecutor commandExecutor, String fromTenantId,
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
        return commandExecutor.execute(new ChangeTenantIdCmmnSimulateCmd(fromTenantId, toTenantId,
                onlyInstancesFromDefaultTenantDefinitions));
    }

    @Override
    public ChangeTenantIdResult complete() {
        return commandExecutor.execute(new ChangeTenantIdCmmnCompleteCmd(fromTenantId, toTenantId,
                onlyInstancesFromDefaultTenantDefinitions));
    }

}
