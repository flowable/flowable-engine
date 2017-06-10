package org.flowable.idm.engine.test.api.identity;

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.authentication.ClearTextPasswordEncoder;
import org.flowable.idm.engine.impl.authentication.SpringEncoder;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

/**
 * Created by faizal on 6/10/17.
 */
public class PasswordEncoderTest extends PluggableFlowableIdmTestCase {

    private static Logger log = LoggerFactory.getLogger(PasswordEncoderTest.class);

    private void validatePassword() {
        User user = idmIdentityService.newUser("johndoe");
        user.setPassword("xxx");
        idmIdentityService.saveUser(user);

        User johndoe = idmIdentityService.createUserQuery().userId("johndoe").list().get(0);
        log.info("Hash Password = {} ", johndoe.getPassword());

        assertFalse("xxx".equals(johndoe.getPassword()));
        assertTrue(idmIdentityService.checkPassword("johndoe", "xxx"));
        assertFalse(idmIdentityService.checkPassword("johndoe", "invalid pwd"));

        idmIdentityService.deleteUser("johndoe");

    }

    public void testValidatePasswordEncoderInstance() {
        PasswordEncoder passwordEncoder = idmIdentityService.getPasswordEncoder();
        assertTrue(passwordEncoder instanceof ClearTextPasswordEncoder);

        idmIdentityService.setPasswordEncoder(new CustomPasswordEncoder());
        passwordEncoder = idmIdentityService.getPasswordEncoder();
        assertTrue(passwordEncoder instanceof CustomPasswordEncoder);
    }

    public void testSpringPasswordEncoderInstance() {

        idmIdentityService.setPasswordEncoder(new SpringEncoder(new Md5PasswordEncoder()));
        validatePassword();

        idmIdentityService.setPasswordEncoder(new SpringEncoder(new StandardPasswordEncoder()));
        validatePassword();
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
