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
package org.flowable.idm.engine.test.api.identity.authentication;

import java.lang.reflect.Method;

import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;

public class JasyptPasswordEncryptor implements PasswordEncoder {

    // org.jasypt.util.password.PasswordEncryptor

    private Object encoder;

    public JasyptPasswordEncryptor(Object encoder) {
        this.encoder = encoder;
    }

    @Override
    public String encode(CharSequence rawPassword, PasswordSalt passwordSalt) {
        Method method = loadMethod("encryptPassword", String.class);
        try {
            return (String) method.invoke(encoder, rawPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean isMatches(CharSequence rawPassword, String encodedPassword, PasswordSalt salt) {
        Method method = loadMethod("checkPassword", String.class, String.class);
        try {
            return (Boolean) method.invoke(encoder, rawPassword, encodedPassword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private Method loadMethod(String methodName, Class... params) {
        try {
            Class<?> aClass = Class.forName("org.jasypt.util.password.PasswordEncryptor");
            return aClass.getMethod(methodName, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
