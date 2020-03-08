package org.flowable.common.engine.impl.interceptor;

public interface InstantiateErrorHandler {
    Object handle(Exception e, Object[] objects);
}
