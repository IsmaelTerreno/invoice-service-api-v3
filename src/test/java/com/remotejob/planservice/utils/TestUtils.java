package com.remotejob.planservice.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.remotejob.planservice.dto.CreateRequest;
import com.remotejob.planservice.dto.ResponseAPI;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Utility class for facilitating tests in a Spring Boot application.
 * Includes methods for performing HTTP requests and handling JSON conversions.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Getter
@Setter
public class TestUtils {
    public static final MediaType CONTENT_TYPE_JSON = MediaType.APPLICATION_JSON;
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_TOKEN_PREFIX = "Bearer ";
    public static final String BASE_JOB_SERVICE_URL = "/api/v1/plan";
    @Autowired
    public MockMvc mockMvc;
    @Value(value = "${jwt.url.endpoint}")
    private String jwtUrlEndpoint;
    @Value(value = "${jwt.secret.refresh}")
    private String jwtSecretRefresh;
    @Value(value = "${jwt.secret.access}")
    private String jwtSecretAccess;
    @Value(value = "${username.test.e2e.client}")
    private String usernameTestE2EClient;
    @Value(value = "${password.test.e2e.client}")
    private String passwordTestE2EClient;
    @Value(value = "${email.test.e2e.client}")
    private String emailTestE2EClient;
    private String authToken;

    // Reusable, properly configured ObjectMapper for tests (handles java.time.* types)
    private final ObjectMapper jsonMapper;

    public TestUtils() {
        this.jsonMapper = new ObjectMapper();
        this.jsonMapper.registerModule(new JavaTimeModule());
        this.jsonMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Converts an object to its JSON representation using Jackson, with proper support
     * for Java Time types like {@link java.time.Instant}.
     *
     * @param <T>    the type of the object to be converted
     * @param object the object to be converted to JSON
     * @return the JSON string representation of the object
     */
    public <T> String convertToJson(T object) {
        try {
            return jsonMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a POST request with the specified content and URL and returns the result.
     *
     * @param content the content to be posted in the request body
     * @param url     the URL to which the request is sent
     * @return the result of the executed request
     * @throws Exception if an error occurs during the request execution
     */
    public ResultActions performPostRequest(String content, String url, String jwtToUse) throws Exception {
        return this.mockMvc.perform(post(url)
                .content(content)
                .header(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + jwtToUse)
                .contentType(CONTENT_TYPE_JSON));
    }

    /**
     * Executes a PUT request with the specified content and URL, and returns the result.
     *
     * @param content the content to be put in the request body
     * @param url     the URL to which the request is sent
     * @return the result of the executed request
     * @throws Exception if an error occurs during the request execution
     */
    public ResultActions performPutRequest(String content, String url, String jwtToUse) throws Exception {
        return this.mockMvc.perform(put(url)
                .content(content)
                .header(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + jwtToUse)
                .contentType(CONTENT_TYPE_JSON));
    }

    /**
     * Executes a PATCH request with the specified content and URL, and returns the result.
     *
     * @param content the content to be included in the request body
     * @param url     the URL to which the request is sent
     * @return the result of the executed request
     * @throws Exception if an error occurs during the request execution
     */
    public ResultActions performPatchRequest(String content, String url, String jwtToUse) throws Exception {
        return this.mockMvc.perform(patch(url)
                .content(content)
                .header(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + jwtToUse)
                .contentType(CONTENT_TYPE_JSON));
    }

    /**
     * Executes a GET request to the specified URL and returns the result.
     *
     * @param url the URL to which the GET request is performed
     * @return the result of the executed GET request
     * @throws Exception if an error occurs during the request execution
     */
    public ResultActions performGetRequest(String url, String jwtToUse) throws Exception {
        return this.mockMvc.perform(get(url)
                .header(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + jwtToUse)
                .contentType(CONTENT_TYPE_JSON));
    }

    /**
     * Executes a DELETE request with the specified content and URL, and returns the result.
     *
     * @param content the content to be included in the request body, or an empty string if the content is null
     * @param url     the URL to which the DELETE request is sent
     * @return the result of the executed DELETE request
     * @throws Exception if an error occurs during the request execution
     */
    public ResultActions performDeleteRequest(String content, String url, String jwtToUse) throws Exception {
        return this.mockMvc.perform(delete(url)
                .content(content == null ? "" : content)
                .header(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + jwtToUse)
                .contentType(CONTENT_TYPE_JSON));
    }

    /**
     * Retrieves the content of the response as a String from the provided MvcResult.
     *
     * @param mvcResult the MvcResult object from which the response content will be extracted
     * @return the content of the response as a String
     * @throws UnsupportedEncodingException if the character encoding is not supported
     */
    public String getStringResponse(MvcResult mvcResult) throws UnsupportedEncodingException {
        return mvcResult.getResponse().getContentAsString();
    }

    /**
     * Retrieves the message from the JSON response encapsulated in an MvcResult.
     *
     * @param <T>          The type of the data contained in the response.
     * @param mvcResult    The MvcResult object containing the response data.
     * @param responseType The class of the expected response data type.
     * @return The message from the response if available, otherwise null.
     * @throws JsonProcessingException      If there is an error processing the JSON.
     * @throws UnsupportedEncodingException If the character encoding is not supported.
     */
    public <T> String getMessageResponse(MvcResult mvcResult, Class<T> responseType) throws JsonProcessingException, UnsupportedEncodingException {
        // Step 1: Convert the MvcResult to a string response.
        String responseString = getStringResponse(mvcResult);

        // Step 3: Deserialize the JSON string into a ResponseAPI<T> object.
        ResponseAPI<T> responseAPI = jsonMapper.readValue(responseString, new TypeReference<ResponseAPI<T>>() {
        });

        // Step 4: Return the message from the ResponseAPI object if it's not null, otherwise return null.
        return responseAPI != null ? responseAPI.getMessage() : null;
    }

    /**
     * Retrieves the data from the JSON response encapsulated in an MvcResult and converts it to the specified response type.
     *
     * @param <T>          The type of the data contained in the response
     * @param mvcResult    The MvcResult object containing the response data
     * @param responseType The class of the expected response data type
     * @return The data from the response if available, otherwise null
     * @throws JsonProcessingException      If there is an error processing the JSON
     * @throws UnsupportedEncodingException If the character encoding is not supported
     */
    public <T> T getDataResponse(MvcResult mvcResult, Class<T> responseType) throws JsonProcessingException, UnsupportedEncodingException {
        // Step 1: Convert the MvcResult to a string response.
        String responseString = getStringResponse(mvcResult);

        // Step 3: Deserialize the JSON string into a ResponseAPI<T> object.
        ResponseAPI<T> responseAPI = jsonMapper.readValue(responseString, new TypeReference<>() {
        });

        // Step 4: Return the message from the ResponseAPI object if it's not null, otherwise return null.
        return responseAPI != null ?
                jsonMapper.convertValue(responseAPI.getData(), responseType) :
                null;

    }

    /**
     * Retrieves a list of data from the JSON response encapsulated in an MvcResult and converts
     * it to the specified response type.
     *
     * @param <T>          the type of the data contained in the response
     * @param mvcResult    the MvcResult object containing the response data
     * @param responseType the class of the expected response data type
     * @return the list of data from the response if available, otherwise an empty list
     * @throws JsonProcessingException      if there is an error processing the JSON
     * @throws UnsupportedEncodingException if the character encoding is not supported
     */
    public <T> List<T> getDataListResponse(MvcResult mvcResult, Class<T> responseType) throws JsonProcessingException, UnsupportedEncodingException {
        // Step 1: Convert the MvcResult to a string response.
        String responseString = getStringResponse(mvcResult);

        // Step 2: Deserialize to a raw ResponseAPI (inner list elements may be LinkedHashMap due to type erasure).
        ResponseAPI<List<Object>> raw = jsonMapper.readValue(responseString, new TypeReference<ResponseAPI<List<Object>>>() {});

        if (raw == null || raw.getData() == null) {
            return Collections.emptyList();
        }

        // Step 3: Convert each element to the desired responseType explicitly.
        return raw.getData().stream()
                .map(elem -> jsonMapper.convertValue(elem, responseType))
                .toList();
    }

    /**
     * Retrieves an authorization token from the authentication server.
     * <p>
     * This method sends an HTTP POST request to the authentication server's token endpoint
     * with the necessary client credentials and audience information. It expects a JSON response
     * containing an access token, which is then extracted and returned.
     *
     * @return The authorization token as a String, or an empty string if the token could not be retrieved.
     */
    public String getAuthToken(String email, String password) throws Exception {
        String token = "";
        try {
            // Create the JSON body for the request.
            String jsonBody = new JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .toString();

            // Create the HttpRequest object.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwtUrlEndpoint + "/oauth/token"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Create the HttpClient object and send the request.
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check the response status and parse the token.
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                token = jsonResponse.getString("access_token");
            } else {
                throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to get auth token : " + e.getMessage());
        }
        return token;
    }

    public String registerApiUserService(String email, String password, String username) throws Exception {
        try {
            // Create the JSON body for the request.
            String jsonBody = new JSONObject()
                    .put("email", email)
                    .put("password", password)
                    .put("username", username)
                    .toString();

            // Create the HttpRequest object.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwtUrlEndpoint + "/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            // Create the HttpClient object and send the request.
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check the response status and parse the token.
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                return jsonResponse.getJSONObject("data").getString("token");
            } else {
                throw new RuntimeException("Failed : HTTP error code : " + response.statusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed registration for user with email " + email + " : " + e.getMessage());
        }
    }


    /**
     * Generates a random alphanumeric string of a specified length.
     *
     * @param length The length of the random string to be generated.
     * @return A randomly generated alphanumeric string.
     */
    public String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (characters.length() * Math.random());
            randomString.append(characters.charAt(index));
        }
        return randomString.toString().concat(".");
    }

    /**
     * Registers a new user and returns a JSON Web Token (JWT) for the registered user.
     * <p>
     * This method generates a create request with random credentials, registers the
     * user via an API call, and returns the JWT upon successful registration.
     * In case of any failure during the registration process, a RuntimeException is thrown.
     *
     * @return the JWT token as a string
     * @throws RuntimeException if the user registration fails
     */
    public String registerUserAndGetJWT() {
        try {
            CreateRequest createRequest = generateCreateRequest();
            return registerApiUserService(createRequest.getEmail(), createRequest.getPassword(), createRequest.getUsername());
        } catch (Exception e) {
            throw new RuntimeException("Failed to register user and get JWT : " + e.getMessage());
        }
    }

    public CreateRequest generateCreateRequest() {
        CreateRequest createRequest = new CreateRequest();
        createRequest.setEmail(generateRandomString(10) + emailTestE2EClient);
        createRequest.setPassword(generateRandomString(10) + passwordTestE2EClient);
        createRequest.setUsername(generateRandomString(10) + usernameTestE2EClient);
        return createRequest;
    }
}
