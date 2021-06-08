package org.flowable.form.engine.impl.cmd;


import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.Key.*;

import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.form.engine.FormEngineConfiguration;
import org.flowable.form.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeTenantIdFormInstanceCompleteCmd implements Command<ChangeTenantIdResult> {

        private final static Logger logger = LoggerFactory.getLogger(ChangeTenantIdFormInstanceCompleteCmd.class);

        private final String sourceTenantId;
        private final String targetTenantId;
        private final boolean onlyInstancesFromDefaultTenantDefinitions;

        public ChangeTenantIdFormInstanceCompleteCmd(String sourceTenantId, String targetTenantId,
                        boolean onlyInstancesFromDefaultTenantDefinitions) {
                this.sourceTenantId = sourceTenantId;
                this.targetTenantId = targetTenantId;
                this.onlyInstancesFromDefaultTenantDefinitions = onlyInstancesFromDefaultTenantDefinitions;
        }

        @Override
        public ChangeTenantIdResult execute(CommandContext commandContext) {
                logger.debug("Executing Form instance migration from '{}' to '{}'{}.", sourceTenantId, targetTenantId,
                                onlyInstancesFromDefaultTenantDefinitions
                                                ? " but only for instances from the default tenant definitions"
                                                : "");
                FormEngineConfiguration formEngineConfiguration = CommandContextUtil.getFormEngineConfiguration(commandContext);
                long changeTenantIdBpmnFormInstances = formEngineConfiguration.getFormInstanceEntityManager()
                                                .changeTenantIdFormInstances(sourceTenantId, targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.BPMN);
                long changeTenantIdCmmnFormInstances = formEngineConfiguration.getFormInstanceEntityManager()
                                                .changeTenantIdFormInstances(sourceTenantId, targetTenantId,
                                                                onlyInstancesFromDefaultTenantDefinitions, ScopeTypes.CMMN);
                return ChangeTenantIdResult.builder()
                                .addResult(FormInstances,changeTenantIdBpmnFormInstances + changeTenantIdCmmnFormInstances)
                                .build();
        }

}
