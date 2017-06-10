package org.flowable.idm.engine.impl.authentication;

import org.flowable.idm.api.PasswordSalt;

public final class BlankSalt implements PasswordSalt {

    private static final BlankSalt INSTANCE = new BlankSalt();

    private BlankSalt() {
    }

    public static BlankSalt getInstance() {
        return INSTANCE;
    }

    @Override
    public Object getSource() {
        return "";
    }

    @Override
    public void setSource(Object source) {
        //nothing
    }
}
