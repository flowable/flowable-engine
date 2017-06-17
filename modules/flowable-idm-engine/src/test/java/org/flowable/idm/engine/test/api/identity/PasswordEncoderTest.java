package org.flowable.idm.engine.test.api.identity;

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.authentication.ApacheDigester;
import org.flowable.idm.engine.impl.authentication.BlankSaltProvider;
import org.flowable.idm.engine.impl.authentication.PasswordSaltImpl;
import org.flowable.idm.engine.impl.authentication.SpringEncoder;
import org.flowable.idm.engine.impl.authentication.SpringSaltProvider;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;
import org.flowable.idm.engine.test.api.identity.authentication.JasyptPasswordEncryptor;
import org.flowable.idm.engine.test.api.identity.authentication.jBCryptHashing;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.dao.SystemWideSaltSource;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    public void testSpringPasswordEncoderInstance() {

        idmIdentityService.setPasswordEncoder(new SpringEncoder(new Md5PasswordEncoder()));
        validatePassword();

        idmIdentityService.setPasswordEncoder(new SpringEncoder(new StandardPasswordEncoder()));
        validatePassword();
    }

    public void testApacheDigesterdEncoderInstance() {

        idmIdentityService.setPasswordEncoder(new ApacheDigester(ApacheDigester.Digester.MD5));
        validatePassword();

        idmIdentityService.setPasswordEncoder(new ApacheDigester(ApacheDigester.Digester.SHA512));
        validatePassword();
    }

    public void testJasptEncoderInstance() {

        idmIdentityService.setPasswordEncoder(new JasyptPasswordEncryptor(new StrongPasswordEncryptor()));
        validatePassword();

    }

    public void testjBCrytpEncoderInstance() {

        idmIdentityService.setPasswordEncoder(new jBCryptHashing());
        validatePassword();

    }

    public void testSaltPasswordEncoderInstance() {

        idmIdentityService.setPasswordEncoder(new SpringEncoder(new StandardPasswordEncoder()));

        User user = idmIdentityService.newUser("johndoe");
        user.setPassword("xxx");
        idmIdentityService.saveUser(user);

        String noSalt = idmIdentityService.createUserQuery().userId("johndoe").list().get(0).getPassword();
        assertTrue(idmIdentityService.checkPassword("johndoe", "xxx"));
        idmIdentityService.deleteUser("johndoe");

        idmIdentityService.setPasswordSalt(new PasswordSaltImpl(BlankSaltProvider.getInstance()));
        user = idmIdentityService.newUser("johndoe1");
        user.setPassword("xxx");
        idmIdentityService.saveUser(user);

        String salt = idmIdentityService.createUserQuery().userId("johndoe1").list().get(0).getPassword();
        assertTrue(idmIdentityService.checkPassword("johndoe1", "xxx"));

        assertFalse(noSalt.equals(salt));
        idmIdentityService.deleteUser("johndoe1");
    }

    public void testSpringSaltPasswordEncoderInstance() {

        idmIdentityService.setPasswordEncoder(new SpringEncoder(new BCryptPasswordEncoder()));

        User user = idmIdentityService.newUser("johndoe");
        user.setPassword("xxx");
        idmIdentityService.saveUser(user);

        String noSalt = idmIdentityService.createUserQuery().userId("johndoe").list().get(0).getPassword();
        assertTrue(idmIdentityService.checkPassword("johndoe", "xxx"));
        idmIdentityService.deleteUser("johndoe");

        SystemWideSaltSource saltSource = new SystemWideSaltSource();
        saltSource.setSystemWideSalt("salt");
        idmIdentityService.setPasswordSalt(new PasswordSaltImpl(new SpringSaltProvider(saltSource)));
        user = idmIdentityService.newUser("johndoe1");
        user.setPassword("xxx");
        idmIdentityService.saveUser(user);

        String salt = idmIdentityService.createUserQuery().userId("johndoe1").list().get(0).getPassword();
        assertTrue(idmIdentityService.checkPassword("johndoe1", "xxx"));

        assertFalse(noSalt.equals(salt));
        idmIdentityService.deleteUser("johndoe1");
    }


    public void testValidatePasswordEncoderInstance() {
        idmIdentityService.setPasswordEncoder(new CustomPasswordEncoder());
        PasswordEncoder passwordEncoder = idmIdentityService.getPasswordEncoder();
        assertTrue(passwordEncoder instanceof CustomPasswordEncoder);
    }


    class CustomPasswordEncoder implements PasswordEncoder {

        public String encode(CharSequence rawPassword, PasswordSalt passwordSalt) {
            return null;
        }

        public boolean isMatches(CharSequence rawPassword, String encodedPassword, PasswordSalt salt) {
            return false;
        }
    }


}
