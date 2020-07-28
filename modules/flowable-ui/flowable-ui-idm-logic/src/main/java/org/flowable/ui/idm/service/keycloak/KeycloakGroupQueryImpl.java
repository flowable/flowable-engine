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
package org.flowable.ui.idm.service.keycloak;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.idm.api.Group;
import org.flowable.idm.engine.impl.GroupQueryImpl;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Filip Hrisafov
 */
public class KeycloakGroupQueryImpl extends GroupQueryImpl {

    protected static final Logger LOGGER = LoggerFactory.getLogger(KeycloakGroupQueryImpl.class);

    protected static final ParameterizedTypeReference<List<KeycloakGroupRepresentation>> KEYCLOAK_LIST_OF_GROUPS = new ParameterizedTypeReference<List<KeycloakGroupRepresentation>>() {

    };

    protected KeycloakConfiguration keycloakConfiguration;

    public KeycloakGroupQueryImpl(KeycloakConfiguration keycloakConfiguration) {
        this.keycloakConfiguration = keycloakConfiguration;
    }

    @Override
    public long executeCount(CommandContext commandContext) {
        //GET /{realm}/groups/count
        // Query parameters: search
        // paging first, max

        UriComponentsBuilder builder = prepareQuery("groups/count");
        URI uri = builder.buildAndExpand(keycloakConfiguration.getRealm()).toUri();

        ResponseEntity<JsonNode> response = keycloakConfiguration.getRestTemplate().getForEntity(uri, JsonNode.class);
        HttpStatus statusCode = response.getStatusCode();
        if (statusCode.is2xxSuccessful()) {
            LOGGER.debug("Successful response from keycloak");
            JsonNode groupsCountNode = response.getBody();
            if (groupsCountNode != null) {
                if (groupsCountNode.isNumber()) {
                    return groupsCountNode.numberValue().longValue();
                }

                return groupsCountNode.path("count").asLong(0);
            } else {
                LOGGER.warn("Keycloak didn't return any body when querying users");
                return 0;
            }
        } else {
            throw new FlowableException("Keycloak returned status code: " + statusCode);
        }
    }

    @Override
    public List<Group> executeList(CommandContext commandContext) {
        List<Group> groups = queryGroups();
        if (getId() != null) {
            String id = getId();
            for (Group group : groups) {
                if (id.equalsIgnoreCase(group.getName())) {
                    return Collections.singletonList(group);
                }
            }

        }
        return groups;
    }

    protected List<Group> queryGroups() {
        // GET /{realm}/users
        // Query parameters: username, email, firstName, lastName, search(email, first, last or username)
        // paging first, max

        UriComponentsBuilder builder = prepareQuery("/groups");

        if (getMaxResults() >= 0) {
            builder.queryParam("max", getMaxResults());
        }

        if (getFirstResult() >= 0) {
            builder.queryParam("first", getFirstResult());
        }

        URI uri = builder.buildAndExpand(keycloakConfiguration.getRealm()).toUri();

        ResponseEntity<List<KeycloakGroupRepresentation>> response = keycloakConfiguration.getRestTemplate()
                .exchange(uri, HttpMethod.GET, null, KEYCLOAK_LIST_OF_GROUPS);

        HttpStatus statusCode = response.getStatusCode();
        if (statusCode.is2xxSuccessful()) {
            LOGGER.debug("Successful response from keycloak");
            List<KeycloakGroupRepresentation> keycloakGroups = response.getBody();
            if (keycloakGroups != null) {
                List<Group> groups = new ArrayList<>(keycloakGroups.size());
                for (KeycloakGroupRepresentation keycloakGroup : keycloakGroups) {
                    addGroup(groups, keycloakGroup);
                }
                return groups;
            } else {
                LOGGER.warn("Keycloak didn't return any body when querying groups");
                return Collections.emptyList();
            }
        } else {
            throw new FlowableException("Keycloak returned status code: " + statusCode);
        }
    }

    protected void addGroup(Collection<Group> groups, KeycloakGroupRepresentation keycloakGroup) {
        Group group = new GroupEntityImpl();
        group.setId(keycloakGroup.getName());
        group.setName(keycloakGroup.getName());
        groups.add(group);
        List<KeycloakGroupRepresentation> subGroups = keycloakGroup.getSubGroups();
        if (subGroups != null && !subGroups.isEmpty()) {
            for (KeycloakGroupRepresentation subGroup : subGroups) {
                addGroup(groups, subGroup);
            }
        }
    }

    protected UriComponentsBuilder prepareQuery(String path) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(keycloakConfiguration.getServer() + "auth/admin/realms/{realm}" + path);

        if (getUserId() != null) {
            // TODO find groups for user
            //builder.queryParam("username", getId());
        } else if (getId() != null) {
            builder.queryParam("search", getId());
        }

        return builder;
    }
}
