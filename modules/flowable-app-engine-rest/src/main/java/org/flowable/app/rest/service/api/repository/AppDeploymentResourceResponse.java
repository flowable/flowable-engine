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
package org.flowable.app.rest.service.api.repository;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class AppDeploymentResourceResponse {
    
    private String id;
    private String url;
    private String contentUrl;
    private String mediaType;
    private String type;

    public AppDeploymentResourceResponse(String resourceId, String url, String contentUrl, String mediaType, String type) {
        setId(resourceId);
        setUrl(url);
        setContentUrl(contentUrl);
        setMediaType(mediaType);

        this.type = type;
        if (type == null) {
            this.type = "resource";
        }
    }

    @ApiModelProperty(example = "oneApp.app")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(value = "For a single resource contains the actual URL to use for retrieving the binary resource", example = "http://localhost:8081/flowable-rest/service/app-repository/deployments/10/resources/oneApp.app")
    public String getUrl() {
        return url;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    @ApiModelProperty(example = "http://localhost:8081/flowable-rest/service/app-repository/deployments/10/resourcedata/oneApp.app")
    public String getContentUrl() {
        return contentUrl;
    }

    public void setMediaType(String mimeType) {
        this.mediaType = mimeType;
    }

    @ApiModelProperty(example = "text/xml", value = "Contains the media-type the resource has. This is resolved using a (pluggable) MediaTypeResolver and contains, by default, a limited number of mime-type mappings.")
    public String getMediaType() {
        return mediaType;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ApiModelProperty(example = "appDefinition", value = "Type of resource", allowableValues = "resource,appDefinition")
    public String getType() {
        return type;
    }
}
