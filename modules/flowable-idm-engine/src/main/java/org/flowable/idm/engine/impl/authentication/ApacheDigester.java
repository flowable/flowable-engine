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
package org.flowable.idm.engine.impl.authentication;

import org.apache.commons.codec.digest.DigestUtils;
import org.flowable.idm.api.PasswordEncoder;
import org.flowable.idm.api.PasswordSalt;

/**
 * @author faizal-manan
 */
public class ApacheDigester implements PasswordEncoder {

    private Digester digester;

    public ApacheDigester(Digester digester) {
        this.digester = digester;
    }

    @Override
    public String encode(CharSequence rawPassword, PasswordSalt passwordSalt) {
        return encodePassword(rawPassword, passwordSalt);
    }

    @Override
    public boolean isMatches(CharSequence rawPassword, String encodedPassword, PasswordSalt salt) {
        return (null == encodedPassword) || encodedPassword.equals(encodePassword(rawPassword, salt));
    }

    public Digester getDigester() {
        return digester;
    }

    private String encodePassword(CharSequence rawPassword, PasswordSalt passwordSalt) {
        String salt = rawPassword + passwordSalt.getSource().getSalt();
        return switch (digester) {
            case MD5 -> DigestUtils.md5Hex(salt);
            case SHA -> DigestUtils.sha1Hex(salt);
            case SHA256 -> DigestUtils.sha256Hex(salt);
            case SHA348 -> DigestUtils.sha384Hex(salt);
            case SHA512 -> DigestUtils.sha512Hex(salt);
            default -> null;
        };
    }

    public enum Digester {
        MD5,
        SHA,
        SHA256,
        SHA348,
        SHA512
    }

}
