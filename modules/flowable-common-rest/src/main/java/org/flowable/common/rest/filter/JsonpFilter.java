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
package org.flowable.common.rest.filter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JsonpFilter {

    protected void afterHandle(HttpServletRequest request, HttpServletResponse response) {
        /*
         * String jsonp = request.getResourceRef().getQueryAsForm().getFirstValue("callback");
         * 
         * if (jsonp != null) { StringBuilder stringBuilder = new StringBuilder(jsonp); stringBuilder.append("(");
         * 
         * if ((response.getStatus().getCode() >= 300)) { stringBuilder.append("{code:"); stringBuilder.append(response.getStatus().getCode()); stringBuilder.append(",msg:'");
         * stringBuilder.append(response.getStatus().getDescription() .replace("'", "\\'")); stringBuilder.append("'}"); response.setStatus(Status.SUCCESS_OK); } else { Representation representation =
         * response.getEntity(); if (representation != null) { try { InputStream is = representation.getStream(); if (is != null) { ByteArrayOutputStream bos = new ByteArrayOutputStream(); byte[] buf
         * = new byte[0x10000]; int len; while ((len = is.read(buf)) > 0) { bos.write(buf, 0, len); } stringBuilder.append(bos.toString("UTF-8")); } else {
         * response.setStatus(Status.SERVER_ERROR_INTERNAL, "NullPointer in Jsonp filter"); } } catch (IOException e) { response.setStatus(Status.SERVER_ERROR_INTERNAL, e.getMessage()); } } }
         * 
         * stringBuilder.append(");"); response.setEntity(new StringRepresentation(stringBuilder.toString(), MediaType.TEXT_JAVASCRIPT)); }
         */
    }
}
