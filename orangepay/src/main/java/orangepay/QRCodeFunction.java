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

import orangepay.OrangeUtils.*;

/**
 * Azure Function with HTTP Trigger for generating QR Code via Orange Money API.
 */
public class QRCodeFunction {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @FunctionName("generateQRCode")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req",
                         methods = {HttpMethod.POST},
                         authLevel = AuthorizationLevel.FUNCTION,
                         dataType = "json") HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
                
        context.getLogger().info("Java HTTP trigger processed a request to generate QR code.");

        // Check if request body is present
        if (!request.getBody().isPresent()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Missing request body").build();
        }

        String jsonRequest = request.getBody().get();
        GenerateQRCodeRequest qrRequest;
        try {
            qrRequest = objectMapper.readValue(jsonRequest, GenerateQRCodeRequest.class);
            // Validate the parsed QR code request
            if (!isValidQRCodeRequest(qrRequest)) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Invalid QR Code request").build();
            }
        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Error parsing request: " + e.getMessage()).build();
        }

        OrangeMoneyApi orangeMoneyApi = new OrangeMoneyApi();
        try {
            String qrCodeResponse = orangeMoneyApi.callOrangeMoneyApi("api/eWallet/v4/qrcode", jsonRequest);
            
            // Wait for the CompletableFuture to complete and return the result
            String responseBody = qrCodeResponse; // Use join() to wait without throwing checked exceptions
            return request.createResponseBuilder(HttpStatus.OK).body(responseBody).build();
        } catch (Exception e) {
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Error calling Orange Money API: " + e.getMessage()).build();
        }
    }

    

   


private boolean isValidQRCodeRequest(GenerateQRCodeRequest qrRequest) {
    // Add your validation logic here
    // Example validation: check if the QR code request fields are not null or empty
    return qrRequest != null && qrRequest.getCode() != null && !qrRequest.getCode().isEmpty();
    // Extend this method to include other validation rules as necessary
}


}
