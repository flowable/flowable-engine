package org.flowable.spring.boot.idm;/* Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.flowable.idm.api.PasswordEncoder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author Filip Hrisafov
 */
public class SpringPasswordEncoderTest {

    private PasswordEncoder underTest;

    private org.springframework.security.crypto.password.PasswordEncoder springPasswordEncoder;

    @Before
    public void setUp() {
        springPasswordEncoder = Mockito.mock(org.springframework.security.crypto.password.PasswordEncoder.class);
        underTest = new SpringPasswordEncoder(springPasswordEncoder);
    }

    @Test
    public void encodePasswordInvokesSpringWrapper() {
        when(springPasswordEncoder.encode("myPassword")).thenReturn("encoded");

        assertThat(underTest.encode("myPassword", null)).isEqualTo("encoded");

        verify(springPasswordEncoder).encode("myPassword");
    }

    @Test
    public void matchesPasswordInvokesSpringWrapper() {
        when(springPasswordEncoder.matches("myPassword", "encoded")).thenReturn(true);
        when(springPasswordEncoder.matches("myPassword2", "encodedOther")).thenReturn(false);

        assertThat(underTest.isMatches("myPassword", "encoded", null)).isEqualTo(true);
        assertThat(underTest.isMatches("myPassword2", "encodedOther", null)).isEqualTo(false);

        verify(springPasswordEncoder).matches("myPassword", "encoded");
        verify(springPasswordEncoder).matches("myPassword2", "encodedOther");
    }
}