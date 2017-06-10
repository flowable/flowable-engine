package org.flowable.idm.engine.impl.authentication;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by faizal on 6/11/17.
 */
public interface EncoderProvider {

    Boolean isAssignedFor(Object obj) throws ClassNotFoundException;

    Object invoke(Object obj, Object... args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException;
}
