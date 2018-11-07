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
package org.flowable.common.rest.multipart;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * {@link org.springframework.web.multipart.MultipartResolver} that allows using PUT for multipart/form
 *
 * @author Filip Hrisafov
 */
public class PutAwareStandardServletMultiPartResolver extends StandardServletMultipartResolver {

    @Override
    public boolean isMultipart(HttpServletRequest request) {
        return request != null && isMultipartContent(request);
    }

    /**
     * Utility method that determines whether the request contains multipart content.
     *
     * @param request
     *     The servlet request to be evaluated. Must be non-null.
     * @return <code>true</code> if the request is multipart; {@code false} otherwise.
     * @see org.apache.commons.fileupload.servlet.ServletFileUpload#isMultipartContent(HttpServletRequest)
     */
    public static final boolean isMultipartContent(HttpServletRequest request) {
        final String method = request.getMethod().toLowerCase();
        if (!method.equalsIgnoreCase("post") && !method.equalsIgnoreCase("put")) {
            return false;
        }

        String contentType = request.getContentType();
        return StringUtils.startsWithIgnoreCase(contentType, "multipart/");
    }
}
