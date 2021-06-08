package org.flowable.dmn.engine.impl.cmd;

import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.Key.*;

import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeTenantIdDmnSimulateCmd implements Command<ChangeTenantIdResult> {

        private final static Logger logger = LoggerFactory.getLogger(ChangeTenantIdDmnSimulateCmd.class);

        private final String sourceTenantId;
        private final String targetTenantId;
        private final boolean onlyInstancesFromDefaultTenantDefinitions;

        public ChangeTenantIdDmnSimulateCmd(String sourceTenantId, String targetTenantId,
                        boolean onlyInstancesFromDefaultTenantDefinitions) {
                this.sourceTenantId = sourceTenantId;
                this.targetTenantId = targetTenantId;
                this.onlyInstancesFromDefaultTenantDefinitions = onlyInstancesFromDefaultTenantDefinitions;
        }

        @Override
        public ChangeTenantIdResult execute(CommandContext commandContext) {
                logger.debug("Simulating DMN migration from '{}' to '{}'{}.", sourceTenantId, targetTenantId,
                                onlyInstancesFromDefaultTenantDefinitions
                                                ? " but only for instances from the default tenant definitions"
                                                : "");
                DmnEngineConfiguration dmnEngineConfiguration = CommandContextUtil.getDmnEngineConfiguration(commandContext);
                return ChangeTenantIdResult.builder()
                                .addResult(HistoricDecisionExecutions, dmnEngineConfiguration
                                                .getHistoricDecisionExecutionEntityManager()
                                                .countChangeTenantIdHistoricDecisionExecutions(sourceTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions))
                                .build();
        }

}

