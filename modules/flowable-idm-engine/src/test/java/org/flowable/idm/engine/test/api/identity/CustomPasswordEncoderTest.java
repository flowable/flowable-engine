package org.flowable.idm.engine.test.api.identity;

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.engine.impl.authentication.ClearTextPasswordEncoder;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;

/**
 * Created by faizal on 6/10/17.
 */
public class CustomPasswordEncoderTest extends PluggableFlowableIdmTestCase {


    public void testValidatePasswordEncoderInstance() {
        PasswordEncoder passwordEncoder = idmIdentityService.getPasswordEncoder();
        assertTrue(passwordEncoder instanceof ClearTextPasswordEncoder);

        idmIdentityService.setPasswordEncoder(new CustomPasswordEncoder());
        passwordEncoder = idmIdentityService.getPasswordEncoder();
        assertTrue(passwordEncoder instanceof CustomPasswordEncoder);
    }

    class CustomPasswordEncoder implements PasswordEncoder {

        public String encode(CharSequence rawPassword) {
            return null;
        }

        public boolean isMatches(CharSequence rawPassword, String encodedPassword) {
            return false;
        }
    }

}
