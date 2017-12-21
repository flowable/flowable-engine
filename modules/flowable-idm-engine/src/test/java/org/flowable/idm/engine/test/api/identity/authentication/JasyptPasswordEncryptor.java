package org.flowable.idm.engine.test.api.identity.authentication;

import java.lang.reflect.Method;

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;

public class JasyptPasswordEncryptor implements PasswordEncoder {

    // org.jasypt.util.password.PasswordEncryptor

    private Object encoder;

    public JasyptPasswordEncryptor(Object encoder) {
        this.encoder = encoder;
    }

    @Override
    public String encode(CharSequence rawPassword, PasswordSalt passwordSalt) {
        Method method = loadMethod("encryptPassword", String.class);
        try {
            return (String) method.invoke(encoder, rawPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isMatches(CharSequence rawPassword, String encodedPassword, PasswordSalt salt) {
        Method method = loadMethod("checkPassword", String.class, String.class);
        try {
            return (Boolean) method.invoke(encoder, rawPassword, encodedPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private Method loadMethod(String methodName, Class... params) {
        try {
            Class<?> aClass = Class.forName("org.jasypt.util.password.PasswordEncryptor");
            return aClass.getMethod(methodName, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
