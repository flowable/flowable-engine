package org.flowable.engine.repository;

import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author Valentin Zickner
 */
public interface DeploymentMergeStrategy {

    void prepareMerge(CommandContext commandContext, String deploymentId, String newTenantId);

    void finalizeMerge(CommandContext commandContext, String deploymentId, String newTenantId);

}
