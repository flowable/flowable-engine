/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.idm.engine.test.api.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.authentication.ApacheDigester;
import org.flowable.idm.engine.impl.authentication.ApacheDigester.Digester;
import org.flowable.idm.engine.impl.authentication.PasswordSaltImpl;
import org.flowable.idm.engine.test.PluggableFlowableIdmTestCase;
import org.flowable.idm.engine.test.api.identity.authentication.JasyptPasswordEncryptor;
import org.flowable.idm.engine.test.api.identity.authentication.jBCryptHashing;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by faizal on 6/10/17.
 */
public class PasswordEncoderTest extends PluggableFlowableIdmTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordEncoderTest.class);

    private void validatePassword() {
        User user = idmIdentityService.newUser("johndoe");
        user.setPassword("xxx");
        idmIdentityService.saveUser(user);

        User johndoe = idmIdentityService.createUserQuery().userId("johndoe").list().get(0);
        LOGGER.info("Hash Password = {}", johndoe.getPassword());

        assertThat(johndoe.getPassword()).isNotEqualTo("xxx");
        assertThat(idmIdentityService.checkPassword("johndoe", "xxx")).isTrue();
        assertThat(idmIdentityService.checkPassword("johndoe", "invalid pwd")).isFalse();

        idmIdentityService.deleteUser("johndoe");
    }

    @Test
    public void testApacheDigesterdEncoderInstance() {
        PasswordEncoder passwordEncoder = idmEngineConfiguration.getPasswordEncoder();

        idmEngineConfiguration.setPasswordEncoder(new ApacheDigester(ApacheDigester.Digester.MD5));
        validatePassword();

        idmEngineConfiguration.setPasswordEncoder(new ApacheDigester(ApacheDigester.Digester.SHA512));
        validatePassword();

        idmEngineConfiguration.setPasswordEncoder(passwordEncoder);
    }

    @Test
    public void testJasptEncoderInstance() {
        PasswordEncoder passwordEncoder = idmEngineConfiguration.getPasswordEncoder();
        idmEngineConfiguration.setPasswordEncoder(new JasyptPasswordEncryptor(new StrongPasswordEncryptor()));
        validatePassword();

        idmEngineConfiguration.setPasswordEncoder(passwordEncoder);
    }

    @Test
    public void testjBCrytpEncoderInstance() {
        PasswordEncoder passwordEncoder = idmEngineConfiguration.getPasswordEncoder();
        idmEngineConfiguration.setPasswordEncoder(new jBCryptHashing());
        validatePassword();

        idmEngineConfiguration.setPasswordEncoder(passwordEncoder);
    }

    @Test
    public void testSaltPasswordEncoderInstance() {
        PasswordEncoder passwordEncoder = idmEngineConfiguration.getPasswordEncoder();
        idmEngineConfiguration.setPasswordEncoder(new ApacheDigester(Digester.MD5));

        User user = idmIdentityService.newUser("johndoe");
        user.setPassword("xxx");
        idmIdentityService.saveUser(user);

        String noSalt = idmIdentityService.createUserQuery().userId("johndoe").list().get(0).getPassword();
        assertThat(idmIdentityService.checkPassword("johndoe", "xxx")).isTrue();
        idmIdentityService.deleteUser("johndoe");

        idmEngineConfiguration.setPasswordSalt(new PasswordSaltImpl("salt"));
        user = idmIdentityService.newUser("johndoe1");
        user.setPassword("xxx");
        idmIdentityService.saveUser(user);

        String salt = idmIdentityService.createUserQuery().userId("johndoe1").list().get(0).getPassword();
        assertThat(idmIdentityService.checkPassword("johndoe1", "xxx")).isTrue();

        assertThat(salt).isNotEqualTo(noSalt);
        idmIdentityService.deleteUser("johndoe1");

        idmEngineConfiguration.setPasswordEncoder(passwordEncoder);
    }

    @Test
    public void testValidatePasswordEncoderInstance() {
        PasswordEncoder passwordEncoder = idmEngineConfiguration.getPasswordEncoder();
        idmEngineConfiguration.setPasswordEncoder(new CustomPasswordEncoder());
        PasswordEncoder customPasswordEncoder = idmEngineConfiguration.getPasswordEncoder();
        assertThat(customPasswordEncoder).isInstanceOf(CustomPasswordEncoder.class);

        idmEngineConfiguration.setPasswordEncoder(passwordEncoder);
    }

    class CustomPasswordEncoder implements PasswordEncoder {

        @Override
        public String encode(CharSequence rawPassword, PasswordSalt passwordSalt) {
            return null;
        }

        @Override
        public boolean isMatches(CharSequence rawPassword, String encodedPassword, PasswordSalt salt) {
            return false;
        }
    }

}
