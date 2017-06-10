package org.flowable.idm.engine.impl.authentication;

import org.flowable.idm.api.PasswordSalt;

public class StringSalt implements PasswordSalt {

    private String salt;

    public StringSalt(String salt) {
        this.salt = salt;
    }

    @Override
    public Object getSource() {
        return salt;
    }

    @Override
    public void setSource(Object source) {
        this.salt = source.toString();
    }
}
