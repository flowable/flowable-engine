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

import org.flowable.idm.api.Picture;
import org.flowable.idm.engine.impl.persistence.entity.UserEntityImpl;
import org.junit.jupiter.api.Test;

/**
 * @author Arkadiy Gornovoy
 */
public class UserEntityTest {

    @Test
    public void testSetPicture_pictureShouldBeSavedWhenNotNull() {
        TestableUserEntity userEntity = new TestableUserEntity();
        Picture picture = new Picture(null, null);
        // even though parameters were null, picture object is not null
        userEntity.setPicture(picture);
        assertThat(userEntity.getHasSavePictureBeenCalled()).isTrue();
        assertThat(userEntity.getHasDeletePictureBeenCalled()).isFalse();
    }

    @Test
    public void testSetPicture_pictureShouldBeDeletedWhenNull() {
        TestableUserEntity userEntity = new TestableUserEntity();
        userEntity.setPicture(null);
        assertThat(userEntity.getHasDeletePictureBeenCalled()).isTrue();
    }

    @SuppressWarnings("serial")
    class TestableUserEntity extends UserEntityImpl {

        private boolean hasSavePictureBeenCalled;
        private boolean hasDeletePictureBeenCalled;

        @Override
        protected void savePicture(Picture picture) {
            setHasSavePictureBeenCalled(true);
        }

        @Override
        protected void deletePicture() {
            setHasDeletePictureBeenCalled(true);
        }

        public boolean getHasSavePictureBeenCalled() {
            return hasSavePictureBeenCalled;
        }

        public void setHasSavePictureBeenCalled(boolean hasSavePictureBeenCalled) {
            this.hasSavePictureBeenCalled = hasSavePictureBeenCalled;
        }

        public boolean getHasDeletePictureBeenCalled() {
            return hasDeletePictureBeenCalled;
        }

        public void setHasDeletePictureBeenCalled(boolean hasDeletePictureBeenCalled) {
            this.hasDeletePictureBeenCalled = hasDeletePictureBeenCalled;
        }

    }

}
