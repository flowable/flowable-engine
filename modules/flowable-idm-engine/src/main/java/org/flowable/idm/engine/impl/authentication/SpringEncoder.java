package org.flowable.idm.engine.impl.authentication;

import org.flowable.idm.api.PasswordEncoder;

public class SpringEncoder implements PasswordEncoder {

    private Object encoder;
    private EncoderProviderContext encoderProviderContext = new EncoderProviderContext();
    private MathchingProviderContext mathchingProviderContext = new MathchingProviderContext();

    public SpringEncoder(Object encoder) {
        this.encoder = encoder;
    }

    @Override
    public String encode(CharSequence rawPassword, String salt) {
        try {
            return encoderProviderContext.invoke(encoder, rawPassword, salt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isMatches(CharSequence rawPassword, String encodedPassword, String salt) {
        try {
            return mathchingProviderContext.invoke(encoder, encodedPassword, rawPassword, salt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
