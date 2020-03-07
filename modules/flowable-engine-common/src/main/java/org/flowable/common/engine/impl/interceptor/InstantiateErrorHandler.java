package org.flowable.common.engine.impl.interceptor;

import org.flowable.common.engine.api.FlowableClassLoadingException;

public interface InstantiateErrorHandler {
    Object handle(Exception e, Object[] objects);
}
