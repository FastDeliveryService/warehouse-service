package com.fds.warehouseservice.support;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

/**
 * Thin fluent wrappers over {@link MockMvc} that always attach a bearer token and JSON content
 * type. Keeps the test classes readable and makes the "who is calling" part of the call site
 * explicit, which matters because authorization is a first-class concern here.
 */
public final class ApiRequests {

    private ApiRequests() {
    }

    public static ResultActions getJson(MockMvc mockMvc, String url, String token) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", TestJwtFactory.bearer(token)));
    }

    public static ResultActions getJson(MockMvc mockMvc, String url) throws Exception {
        return mockMvc.perform(get(url));
    }

    public static ResultActions postJson(MockMvc mockMvc, String url, Object body, String token) throws Exception {
        return mockMvc.perform(post(url)
                .header("Authorization", TestJwtFactory.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(Json.write(body)));
    }

    public static ResultActions postJson(MockMvc mockMvc, String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(Json.write(body)));
    }

    public static ResultActions postEmpty(MockMvc mockMvc, String url, String token) throws Exception {
        return mockMvc.perform(post(url).header("Authorization", TestJwtFactory.bearer(token)));
    }

    public static ResultActions putJson(MockMvc mockMvc, String url, Object body, String token) throws Exception {
        return mockMvc.perform(put(url)
                .header("Authorization", TestJwtFactory.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(Json.write(body)));
    }

    public static ResultActions patchJson(MockMvc mockMvc, String url, Object body, String token) throws Exception {
        return mockMvc.perform(patch(url)
                .header("Authorization", TestJwtFactory.bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(Json.write(body)));
    }

    public static ResultActions patchEmpty(MockMvc mockMvc, String url, String token) throws Exception {
        return mockMvc.perform(patch(url).header("Authorization", TestJwtFactory.bearer(token)));
    }

    public static ResultActions deleteJson(MockMvc mockMvc, String url, String token) throws Exception {
        return mockMvc.perform(delete(url).header("Authorization", TestJwtFactory.bearer(token)));
    }
}
