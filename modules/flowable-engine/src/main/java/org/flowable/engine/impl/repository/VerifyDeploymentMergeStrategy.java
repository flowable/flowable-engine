package org.flowable.engine.impl.repository;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.repository.DeploymentMergeStrategy;

/**
 * @author Valentin Zickner
 */
public class VerifyDeploymentMergeStrategy implements DeploymentMergeStrategy {

    @Override
    public void prepareMerge(CommandContext commandContext, String deploymentId, String newTenantId) {
    }

    @Override
    public void finalizeMerge(CommandContext commandContext, String deploymentId, String newTenantId) {
    }

}
