package org.flowable.idm.api;

public interface PasswordEncoder {

    String encode(CharSequence rawPassword, String salt);

    boolean isMatches(CharSequence rawPassword, String encodedPassword, String salt);

}
