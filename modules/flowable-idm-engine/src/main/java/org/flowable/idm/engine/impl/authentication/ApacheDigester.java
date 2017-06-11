package org.flowable.idm.engine.impl.authentication;

import org.flowable.idm.api.PasswordEncoder;

import java.lang.reflect.Method;

public class ApacheDigester implements PasswordEncoder {


    private Digester digester;

    public ApacheDigester(Digester digester) {
        this.digester = digester;
    }


    @Override
    public String encode(CharSequence rawPassword, String salt) {
        return encodePassword(rawPassword, salt);
    }

    @Override
    public boolean isMatches(CharSequence rawPassword, String encodedPassword, String salt) {
        return (null == encodedPassword) ? true : encodedPassword.equals(encodePassword(rawPassword, salt));
    }

    public Digester getDigester() {
        return digester;
    }

    private String encodePassword(CharSequence rawPassword, String salt) {
        try {
            Class<?> aClass = Class.forName("org.apache.commons.codec.digest.DigestUtils");
            Method method = aClass.getMethod(digester.methodName, String.class);
            CharSequence pass = (null == salt) ? "" : salt;
            return (String) method.invoke(digester, rawPassword + salt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public enum Digester {
        MD5("md5Hex"),
        SHA("sha1Hex"),
        SHA256("sha256Hex"),
        SHA348("sha384Hex"),
        SHA512("sha512Hex");

        String methodName;

        Digester(String methodName) {
            this.methodName = methodName;
        }
    }

}
