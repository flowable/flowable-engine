package org.flowable.rest.conf;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.flowable.engine.impl.util.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.client.RestTemplate;

/**
 * Filter used to authenticate seal x-session-token also allow access from any
 * hosts
 * 
 * @author Ibrahim.El-Nemr
 *
 */
public class XSessionTokenSecuirtyFilter implements Filter {

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		response.addHeader("Access-Control-Allow-Origin", request.getHeader("ORIGIN"));
		response.addHeader("Access-Control-Allow-Credentials", "true");
		response.addHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
		response.addHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS");

		if (null == request.getHeader("X-Session-Token"))
			throw new BadCredentialsException("Authentication failed for this request");

		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.add("X-Session-Token", request.getHeader("X-Session-Token"));

		HttpEntity<String> entity = new HttpEntity<String>("parameters", headers);

		ResponseEntity<String> result;

		try {
			result = restTemplate.exchange("http://scdweb:8080/seal-ws/v5/users/me", HttpMethod.GET, entity,
					String.class);
		} catch (Exception e) {
			throw new BadCredentialsException("Authentication failed for this request");
		}

		if (!result.getStatusCode().toString().equals("200"))
			throw new BadCredentialsException("Authentication failed for this request");

		JSONObject myObj = new JSONObject(result.getBody());

		String name = myObj.getString("username");

		if (name != null && !name.equals("")) {
			chain.doFilter(req, res);
		} else {
			throw new BadCredentialsException("Authentication failed for this username and password");
		}

	}

	public void init(FilterConfig filterConfig) {
	}

	public void destroy() {
	}

}