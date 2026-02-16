/*
 * Copyright 2026, Flowable Licences AG.
 * This license is based on the software license agreement and terms and conditions in effect between the parties
 * at the time of purchase of the Flowable software product.
 * Your agreement to these terms and conditions is required to install or use the Flowable software product and/or this file.
 * Flowable is a trademark of Flowable AG registered in several countries.
 */
package org.flowable.common.engine.api.definition;

import java.util.Set;

import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * A {@link VariableContainer} that carries definition context (definitionId, deploymentId, scopeType, tenantId)
 * for expression evaluation when no execution is active (e.g. during deployment).
 * This allows EL resolvers to look up the parent deployment and resolve definition-scoped expressions.
 *
 * @author Christopher Welsch
 */
public class DefinitionVariableContainer implements VariableContainer {

    protected String definitionId;
    protected String deploymentId;
    protected String scopeType;
    protected String tenantId;

    public DefinitionVariableContainer() {
    }

    public DefinitionVariableContainer(String definitionId, String deploymentId, String scopeType, String tenantId) {
        this.definitionId = definitionId;
        this.deploymentId = deploymentId;
        this.scopeType = scopeType;
        this.tenantId = tenantId;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getScopeType() {
        return scopeType;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public boolean hasVariable(String variableName) {
        return false;
    }

    @Override
    public Object getVariable(String variableName) {
        return null;
    }

    @Override
    public void setVariable(String variableName, Object variableValue) {

    }

    @Override
    public void setTransientVariable(String variableName, Object variableValue) {

    }

    @Override
    public Set<String> getVariableNames() {
        return Set.of();
    }
}
