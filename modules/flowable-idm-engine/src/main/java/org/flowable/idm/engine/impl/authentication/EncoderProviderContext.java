package org.flowable.idm.engine.impl.authentication;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public final class EncoderProviderContext {

    private List<EncoderProvider> encoderProviders;

    public EncoderProviderContext() {
        this.encoderProviders = new ArrayList<>();
        encoderProviders.add(new SpringEncodingPasswordEncoder());
        encoderProviders.add(new SpringCrytoPasswordEncoder());
    }

    public String invoke(Object obj, Object... args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        for (EncoderProvider encoderProvider : encoderProviders) {
            if (encoderProvider.isAssignedFor(obj))
                return (String) encoderProvider.invoke(obj, args);
        }
        return null;
    }
}
