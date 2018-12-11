package org.flowable.ui.idm.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.json.JSONObject;
import org.springframework.http.converter.json.GsonFactoryBean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SSOUserInfo {

    private String id;
    private String firstName;
    private String lastName;
    private String email;

    private String tenant;
    private List<String> privileges;

    public SSOUserInfo(){
    }

    public SSOUserInfo(String id, String firstName, String lastName, String email) {
        this(id, firstName, lastName, email, null, id);
    }

    public SSOUserInfo(String id, String firstName, String lastName, String email, String tenant) {
        this(id, firstName, lastName, email, null, tenant);
    }

    public SSOUserInfo(String id, String firstName, String lastName, String email, List<String> privileges) {
        this(id, firstName, lastName, email, privileges, id);
    }

    public SSOUserInfo(String id, String firstName, String lastName, String email, List<String> privileges, String tenant) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;

        this.privileges = privileges;
        this.tenant = tenant;
    }

    public String getId() {
        return id;
    }
    public String getFirstName() {
        return firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public String getEmail() {
        return email;
    }
    public String getTenant() {
        return tenant;
    }
    public List<String> getPrivileges() {
        return privileges;
    }

    public void setId(String id) {
        this.id = id;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }
    public void setPrivileges(List<String> privileges) {
        this.privileges = privileges;
    }
}
