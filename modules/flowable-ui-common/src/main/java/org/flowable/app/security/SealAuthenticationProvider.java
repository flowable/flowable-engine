/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.app.security;

import java.util.Collection;
import java.util.HashSet;

import org.flowable.idm.api.User;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author ahmedghonim
 */
public class SealAuthenticationProvider implements AuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SealAuthenticationProvider.class);
    @Override
    public Authentication authenticate(final Authentication a) throws AuthenticationException {
        LOGGER.error("YYYYYYY Authenticating");
        User u = new User() {
            @Override
            public String getId() {
                return a.getPrincipal().toString();
            }

            @Override
            public void setId(String id) {

            }

            @Override
            public String getFirstName() {
                return a.getPrincipal().toString();
            }

            @Override
            public void setFirstName(String firstName) {

            }

            @Override
            public void setLastName(String lastName) {

            }

            @Override
            public String getLastName() {
                return a.getPrincipal().toString();
            }

            @Override
            public void setEmail(String email) {

            }

            @Override
            public String getEmail() {
                return a.getPrincipal().toString();
            }

            @Override
            public String getPassword() {
                return a.getCredentials().toString();
            }

            @Override
            public void setPassword(String string) {

            }

            @Override
            public boolean isPictureSet() {
                return false;
            }
        };
        Collection<GrantedAuthority> auth = new HashSet<>();

        String sealBase = "http://scdweb:8080/seal-ws";
        String nonce = "http://scdweb:8080/seal-ws/v5/security/nonce";

        if (!a.getPrincipal().toString().equals("admin")) {

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);
            ResponseEntity<String> result = restTemplate.exchange(nonce, HttpMethod.GET, entity, String.class);
            if (!result.getStatusCode().toString().equals("200")) {
                throw new BadCredentialsException("Authentication failed for this request with nonce");
            }
            String actualNonce = result.getBody();
            JSONObject request = new JSONObject();
            try {
                request.put("principal", a.getPrincipal().toString());
                request.put("password", a.getCredentials().toString());
                request.put("nonce", actualNonce);
            } catch (Exception e) {
                e.printStackTrace();
            }

            HttpHeaders headers2 = new HttpHeaders();
            headers2.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity2 = new HttpEntity<String>(request.toString(), headers2);
            ResponseEntity<String> loginResponse = restTemplate.exchange(sealBase + "/v5/auths", HttpMethod.POST, entity2, String.class);
            if (!loginResponse.getStatusCode().toString().equals("201")) {
                throw new BadCredentialsException("Authentication failed for this request with auths");
            }
        }

        FlowableAppUser user = new FlowableAppUser(u, a.getPrincipal().toString(), auth);

        return new UsernamePasswordAuthenticationToken(user, a.getCredentials().toString(), auth);
    }

    @Override
    public boolean supports(Class<?> type) {
        return true;
    }

}
