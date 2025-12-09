/*
 * Copyright 2006-2009 Odysseus Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.impl.de.odysseus.el.misc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessagesTest {

    /*
     * Test method for 'de.odysseus.el.lang.Messages.get(String)'
     */
    @Test
    void testGetString() {
        assertThat(LocalMessages.get("foo")).matches(".*foo");
    }

    /*
     * Test method for 'de.odysseus.el.lang.Messages.get(String, Object)'
     */
    @Test
    void testGetStringObject() {
        assertThat(LocalMessages.get("foo", "bar")).matches(".*foo\\(bar\\)");
    }

    /*
     * Test method for 'de.odysseus.el.lang.Messages.get(String, Object, Object)'
     */
    @Test
    void testGetStringObjectObject() {
        assertThat(LocalMessages.get("foo", "bar", "baz")).matches(".*foo\\(bar,\\s*baz\\)");
    }
}
