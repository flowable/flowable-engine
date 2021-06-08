package org.flowable.content.engine.impl.cmd;


import static org.flowable.common.engine.api.tenant.ChangeTenantIdResult.Key.*;

import org.flowable.common.engine.api.tenant.ChangeTenantIdResult;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeTenantIdContentSimulateCmd implements Command<ChangeTenantIdResult> {

        private final static Logger logger = LoggerFactory.getLogger(ChangeTenantIdContentSimulateCmd.class);

        private final String sourceTenantId;
        private final String targetTenantId;

        public ChangeTenantIdContentSimulateCmd(String sourceTenantId, String targetTenantId) {
                this.sourceTenantId = sourceTenantId;
                this.targetTenantId = targetTenantId;
        }

        @Override
        public ChangeTenantIdResult execute(CommandContext commandContext) {
                logger.debug("Simulating Content Instance migration from '{}' to '{}'.", sourceTenantId, targetTenantId);
                ContentEngineConfiguration contentEngineConfiguration = CommandContextUtil.getContentEngineConfiguration(commandContext);
                long changeTenantIdContentItemInstances = contentEngineConfiguration.getContentItemEntityManager()
                                                .countChangeTenantIdContentItemInstances(sourceTenantId);
                return ChangeTenantIdResult.builder()
                                .addResult(ContentItemInstances,changeTenantIdContentItemInstances)
                                .build();
        }

}



