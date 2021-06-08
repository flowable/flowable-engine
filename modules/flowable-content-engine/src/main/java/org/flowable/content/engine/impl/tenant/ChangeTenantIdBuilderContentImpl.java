package org.flowable.content.engine.impl.tenant;

import org.flowable.common.engine.api.tenant.ChangeTenantIdBuilder;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.content.engine.impl.cmd.ChangeTenantIdContentCompleteCmd;
import org.flowable.content.engine.impl.cmd.ChangeTenantIdContentSimulateCmd;

public class ChangeTenantIdBuilderContentImpl implements ChangeTenantIdBuilder {

    private final CommandExecutor commandExecutor;
    private final String fromTenantId;
    private final String toTenantId;

    public ChangeTenantIdBuilderContentImpl(CommandExecutor commandExecutor, String fromTenantId,
            String toTenantId) {
        this.commandExecutor = commandExecutor;
        this.fromTenantId = fromTenantId;
        this.toTenantId = toTenantId;
    }

    @Override
    public ChangeTenantIdBuilder onlyInstancesFromDefaultTenantDefinitions(
        boolean onlyInstancesFromDefaultTenantDefinitionsEnabled) {
        if (onlyInstancesFromDefaultTenantDefinitionsEnabled) {
            throw new UnsupportedOperationException("Content items do not have definitions. Unsupported builder option.");
        }
        return this;
    }

    @Override
    public ChangeTenantIdResult simulate() {
        return commandExecutor.execute(new ChangeTenantIdContentSimulateCmd(fromTenantId, toTenantId));
    }

    @Override
    public ChangeTenantIdResult complete() {
        return commandExecutor.execute(new ChangeTenantIdContentCompleteCmd(fromTenantId, toTenantId));
    }

}

