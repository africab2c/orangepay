package orangepay;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import orangepay.OrangeUtils.*;

public class OrangeMoneyApi {

    private static final String OM_BASE_URL = System.getenv("OM_BASE_URL") != null
            ? System.getenv("OM_BASE_URL")
            : "https://api.sandbox.orange-sonatel.com/";

    // Ensuring the endpoint URL ends with a slash for proper concatenation
    private static final String OM_BASE_ENDPOINT = OM_BASE_URL.endsWith("/") ? OM_BASE_URL : OM_BASE_URL + "/";

    private static final String OM_CLIENT_ID = System.getenv("ORANGE_MONEY_CLIENT_ID");
    private static final String OM_CLIENT_SECRET = System.getenv("ORANGE_MONEY_CLIENT_SECRET");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String getToken() throws Exception {
        
        HttpClient client = HttpClient.newHttpClient();
        String params = "grant_type=client_credentials&client_id=" + OM_CLIENT_ID + "&client_secret="
                + OM_CLIENT_SECRET;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sandbox.orange-sonatel.com/oauth/token")) // Correctly using OM_BASE_ENDPOINT
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(params))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) { // Ensure the request was successful
            JsonNode rootNode = objectMapper.readTree(response.body());
            return rootNode.path("access_token").asText();
        } else {
            throw new RuntimeException("Failed to retrieve token. Status code: " + response.statusCode() + " Response: "
                    + response.body());
        }
    }

    public PublicKeyResponse getPublicKey() throws Exception {
        String token = getToken(); // Retrieve the token internally

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.sandbox.orange-sonatel.com/" + "api/account/v1/publicKeys"))
                .header("Authorization", "Bearer " + token) // Use the token
                .header("Content-Type", "application/json")
                .GET() // Assuming GET request
                .build();

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Deserialize the JSON response into PublicKeyResponse object
            PublicKeyResponse publicKeyResponse = objectMapper.readValue(response.body(), PublicKeyResponse.class);
            return publicKeyResponse; // Or return publicKeyResponse.getKey() to return only the key string
        } else {
            // Handle error responses appropriately
            throw new RuntimeException("Failed to retrieve public key: Status code " + response.statusCode());
        }
    }

    public String callOrangeMoneyApi(String method, String jsonRequest)
            throws IOException, InterruptedException {
        // URL from environment variable or hardcoded

        if (OM_BASE_ENDPOINT == null || OM_BASE_ENDPOINT.trim().isEmpty()) {
            throw new IllegalStateException("OM_BASE_URL environment variable is not set.");
        }

        String apiUrl = OM_BASE_ENDPOINT + method;
        try {
            String token = getToken(); // Assume getToken is implemented in OrangeMoneyApi
            // Prepare and send the HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token) // Assuming Bearer token auth
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

            // Check the response status code
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                // Success, return the response body
                return response.body();
            } else {
                // Handle non-successful response
                throw new RuntimeException("Failed to call Orange Money API: Status code " + response.statusCode()
                        + ", Body: " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Orange Money API: Status code ");
        }
    }

    public CompletableFuture<String> callOrangeMoneyApiAsync(String method, String jsonRequest, String token) {

        if (method == null || method.trim().isEmpty()) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("API URL parameter is missing or empty."));
            return future;
        }
        String apiUrl = OM_BASE_ENDPOINT + method;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpClient client = HttpClient.newHttpClient();

        return client.sendAsync(request, BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return response.body();
                    } else {
                        throw new RuntimeException("Failed to call Orange Money API: Status code "
                                + response.statusCode() + ", Body: " + response.body());
                    }
                });
    }

    public String sendPaymentRequest(OneStepPaymentRequest paymentRequest, String token) throws Exception {
        // Serialize the request object to JSON
        String jsonRequest = objectMapper.writeValueAsString(paymentRequest);

        // Construct the API URL from environment variables or configuration
        String apiUrl = System.getenv("ORANGE_MONEY_PAYMENT_API_URL");

        // Send the request
        // Assuming callOrangeMoneyApiAsync or a synchronous version is implemented to
        // handle API calls
        String response = callOrangeMoneyApi(apiUrl, jsonRequest);

        return response;
    }

    public String generateOTP(OrangeUtils.GenerateOTPRequest request) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(OM_BASE_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(convertRequestToJson(request)))
                    .build();

            HttpResponse<String> response = client.send(httpRequest, BodyHandlers.ofString());

            // Check response status code and handle response
            if (response.statusCode() == 200) {
                // Assuming the response body contains the OTP information
                return response.body(); // or process the response as needed
            } else {
                // Handle non-200 responses
                return "Failed to generate OTP: " + response.statusCode();
            }
        } catch (Exception e) {
            return "Error calling Orange API: " + e.getMessage();
        }
    }

    private String convertRequestToJson(GenerateOTPRequest request) throws Exception {
        ObjectMapper mapper = new ObjectMapper(); // Jackson's ObjectMapper
        return mapper.writeValueAsString(request);
    }

}
