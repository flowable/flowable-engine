package org.flowable.idm.engine.impl.authentication;

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;

public class SpringEncoder implements PasswordEncoder {

    private org.springframework.security.authentication.encoding.PasswordEncoder encodingPasswordEncoder;
    private org.springframework.security.crypto.password.PasswordEncoder cryptoPasswordEncoder;

    public SpringEncoder(org.springframework.security.authentication.encoding.PasswordEncoder passwordEncoder) {
        this.encodingPasswordEncoder = passwordEncoder;
    }

    public SpringEncoder(org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.cryptoPasswordEncoder = passwordEncoder;
    }

    @Override
    public String encode(CharSequence rawPassword, PasswordSalt passwordSalt) {
        if (null == encodingPasswordEncoder)
            return cryptoPasswordEncoder.encode(rawPassword);
        else return encodingPasswordEncoder.encodePassword(rawPassword.toString(), passwordSalt);
    }

    @Override
    public boolean isMatches(CharSequence rawPassword, String encodedPassword, PasswordSalt passwordSalt) {
        if (null == encodingPasswordEncoder)
            return cryptoPasswordEncoder.matches(rawPassword, encodedPassword);
        else
            return encodingPasswordEncoder.isPasswordValid(encodedPassword, rawPassword.toString(), passwordSalt);
    }
}
