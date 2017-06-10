package org.flowable.idm.engine.impl.authentication;

import org.flowable.idm.api.PasswordEncoder;

public final class ClearTextPasswordEncoder implements PasswordEncoder {

    private static final PasswordEncoder INSTANCE = new ClearTextPasswordEncoder();

    private ClearTextPasswordEncoder() {
    }

    public static PasswordEncoder getInstance() {
        return INSTANCE;
    }

    public String encode(CharSequence rawPassword, String salt) {
        return (null == rawPassword) ? null : rawPassword.toString();
    }

    public boolean isMatches(CharSequence rawPassword, String encodedPassword, String salt) {
        return rawPassword.toString().equals(encodedPassword);
    }

}
