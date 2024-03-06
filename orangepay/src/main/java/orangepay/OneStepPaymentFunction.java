package orangepay;

import com.microsoft.azure.functions.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;

import java.util.Optional;

public class OneStepPaymentFunction {
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Declare and initialize ObjectMapper here
    private static final OrangeMoneyApi orangeMoneyApi = new OrangeMoneyApi();

    @FunctionName("processOneStepPayment")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", 
                         methods = {HttpMethod.POST}, 
                         authLevel = AuthorizationLevel.FUNCTION, 
                         dataType = "json") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        
        context.getLogger().info("Processing a one-step payment request.");

        if (!request.getBody().isPresent() || request.getBody().get().isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Missing or empty request body").build();
        }

        String jsonRequest = request.getBody().get();

        // Here you would validate the JSON structure. This example assumes you have a method for that.
        // For instance, ensure required fields are present and properly formatted.
        if (!isValidJsonRequest(jsonRequest)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Invalid JSON request structure").build();
        }

        try {
            // Retrieve the token
            // Define the API URL for the one-step payment endpoint. Replace <OneStepPaymentApiUrl> with the actual URL.
            String method = "api/eWallet/v1/payments/onestep";
            String paymentResponse = orangeMoneyApi.callOrangeMoneyApi(method, jsonRequest);
            // Assuming paymentResponse contains the result of the payment process
            return request.createResponseBuilder(HttpStatus.OK).body(paymentResponse).build();
        } catch (Exception e) {
            context.getLogger().severe("Failed to process payment: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process payment: " + e.getMessage()).build();
        }
    }

    private boolean isValidJsonRequest(String jsonRequest) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonRequest);
            
            // Validate 'amount' object structure
            if (!rootNode.hasNonNull("amount") || !rootNode.path("amount").hasNonNull("value") || rootNode.path("amount").path("value").floatValue() < 1) {
                return false;
            }
            
            // Validate 'customer' object structure
            if (!rootNode.hasNonNull("customer") || !rootNode.path("customer").hasNonNull("id") || !rootNode.path("customer").path("id").asText().matches("\\d{9}")) {
                return false;
            }
            
            if (!rootNode.path("customer").hasNonNull("idType") || (!rootNode.path("customer").path("idType").asText().equals("CODE") && !rootNode.path("customer").path("idType").asText().equals("MSISDN"))) {
                return false;
            }
            
            if (!rootNode.path("customer").hasNonNull("otp") || !rootNode.path("customer").path("otp").asText().matches("\\d{6}")) {
                return false;
            }
            
            // Validate 'partner' object structure
            if (!rootNode.hasNonNull("partner") || !rootNode.path("partner").hasNonNull("id") || !rootNode.path("partner").path("id").asText().matches("\\d{6}")) {
                return false;
            }
            
            if (!rootNode.path("partner").hasNonNull("idType") || (!rootNode.path("partner").path("idType").asText().equals("CODE") && !rootNode.path("partner").path("idType").asText().equals("MSISDN"))) {
                return false;
            }
            
            // Validate 'reference' field
            if (!rootNode.hasNonNull("reference")) {
                return false;
            }

            // Optionally validate 'metadata' object structure if required
            if (rootNode.hasNonNull("metadata")) {
                JsonNode metadataNode = rootNode.get("metadata");
                if (!metadataNode.isObject() || metadataNode.size() > 10) {
                    // Assuming 'metadata' should be an object with no more than 10 key-value pairs
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false; // JSON is invalid if parsing fails or checks do not pass
        }
    }

  
}
