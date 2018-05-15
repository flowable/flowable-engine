package org.flowable.common.engine.impl;

import org.flowable.common.engine.impl.el.ExpressionManager;

/**
 * author martin.grofcik
 */
public interface HasExpressionManagerEngineConfiguration {

    ExpressionManager getExpressionManager();

    AbstractEngineConfiguration setExpressionManager(ExpressionManager expressionManager);
}
