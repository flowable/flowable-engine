package org.flowable.idm.engine.impl.authentication;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public final class MathchingProviderContext {

    private List<EncoderProvider> encoderProviders;

    public MathchingProviderContext() {
        this.encoderProviders = new ArrayList<>();
        encoderProviders.add(new SpringEncodingPasswordMatcher());
        encoderProviders.add(new SpringCrytoPasswordMatcher());
    }

    public Boolean invoke(Object obj, Object... args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        for (EncoderProvider encoderProvider : encoderProviders) {
            if (encoderProvider.isAssignedFor(obj))
                return (Boolean) encoderProvider.invoke(obj, args);
        }
        return null;
    }
}
