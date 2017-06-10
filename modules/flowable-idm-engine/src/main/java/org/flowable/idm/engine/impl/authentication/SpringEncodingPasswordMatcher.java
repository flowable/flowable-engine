package org.flowable.idm.engine.impl.authentication;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by faizal on 6/11/17.
 */
public class SpringEncodingPasswordMatcher implements EncoderProvider {

    private String className = "org.springframework.security.authentication.encoding.PasswordEncoder";
    private String methodName = "isPasswordValid";

    @Override
    public Boolean isAssignedFor(Object obj) throws ClassNotFoundException {
        return Class.forName(className).isAssignableFrom(obj.getClass());
    }

    @Override
    public Object invoke(Object obj, Object... args) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class aClass = Class.forName(className);
        Method method = aClass.getMethod(methodName, String.class, String.class, Object.class);
        return method.invoke(obj, args);
    }
}
