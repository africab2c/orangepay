package orangepay;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

public class OSPFunction {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final OrangeMoneyApi orangeMoneyApi = new OrangeMoneyApi();

    @FunctionName("processOneStepPayment")
    public HttpResponseMessage processOneStepPayment(
            @HttpTrigger(name = "req",
                         methods = {HttpMethod.POST},
                         authLevel = AuthorizationLevel.FUNCTION,
                         dataType = "json") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing a one-step payment request.");

        // Extract and validate the JSON request body
        String jsonRequest = request.getBody().orElse("");
        if (jsonRequest.isBlank()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Missing request body").build();
        }

        // Optionally, validate JSON structure here (simple validation example)
        if (!isValidJsonRequest(jsonRequest)) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Invalid request structure").build();
        }

        try {
            String method = "api/eWallet/v1/payments/onestep"; // Specify the actual API URL
            String paymentResponse = orangeMoneyApi.callOrangeMoneyApi(method, jsonRequest);
            return request.createResponseBuilder(HttpStatus.OK).body(paymentResponse).build();
        } catch (Exception e) {
            context.getLogger().severe("Failed to process payment: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process payment").build();
        }
    }

    // Simple JSON structure validation example
    private boolean isValidJsonRequest(String jsonRequest) {
        try {
            objectMapper.readTree(jsonRequest);
            return true; // JSON is valid if it can be parsed
        } catch (Exception e) {
            return false; // JSON is invalid
        }
    }
}
