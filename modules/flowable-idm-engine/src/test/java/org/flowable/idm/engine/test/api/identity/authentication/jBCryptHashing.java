package org.flowable.idm.engine.test.api.identity.authentication;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;

public class jBCryptHashing implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword, PasswordSalt passwordSalt) {
        Method method = loadMethod("hashpw", String.class, String.class);
        try {
            return (String) method.invoke(null, rawPassword, gensalt());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isMatches(CharSequence rawPassword, String encodedPassword, PasswordSalt salt) {
        Method method = loadMethod("checkpw", String.class, String.class);
        try {
            return (Boolean) method.invoke(null, rawPassword, encodedPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return false;
    }

    private String gensalt() throws InvocationTargetException, IllegalAccessException {
        Method method = loadMethod("gensalt");
        return (String) method.invoke(null);
    }


    private Method loadMethod(String methodName, Class... params) {
        try {
            Class<?> aClass = Class.forName("org.mindrot.jbcrypt.BCrypt");
            return aClass.getMethod(methodName, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
