package org.flowable.idm.engine.impl.authentication;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by faizal on 6/11/17.
 */
public class SpringCrytoPasswordMatcher implements EncoderProvider {

    private String className = "org.springframework.security.crypto.password.PasswordEncoder";
    private String methodName = "matches";

    @Override
    public Boolean isAssignedFor(Object obj) throws ClassNotFoundException {
        return Class.forName(className).isAssignableFrom(obj.getClass());
    }

    @Override
    public Object invoke(Object obj, Object... args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class aClass = Class.forName(className);
        Method method = aClass.getMethod(methodName, CharSequence.class, String.class);
        return method.invoke(obj, args[1], args[0]);
    }
}
