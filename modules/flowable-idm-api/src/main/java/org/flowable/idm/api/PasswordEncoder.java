package org.flowable.idm.api;

public interface PasswordEncoder {

    String encode(CharSequence rawPassword);

    boolean isMatches(CharSequence rawPassword, String encodedPassword);

}
