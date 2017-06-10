package org.flowable.idm.engine.impl.authentication;

import org.flowable.idm.api.PasswordSalt;

public class SpringSalt implements PasswordSalt {

    private Object salt;

    public SpringSalt(Object salt) {
        this.salt = salt;
    }

    @Override
    public Object getSource() {
        return salt;
    }

    @Override
    public void setSource(Object source) {
        this.salt = source;
    }
}
