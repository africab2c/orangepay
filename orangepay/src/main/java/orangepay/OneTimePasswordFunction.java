package orangepay;

import com.microsoft.azure.functions.annotation.*;

import orangepay.OrangeUtils.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.*;

/**
 * Azure Function for generating OTP for Payment.
 */
public class OneTimePasswordFunction {
    @FunctionName("createOTPForPayment")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.POST }, authLevel = AuthorizationLevel.FUNCTION, dataType = "json") HttpRequestMessage<OTPRequest> request,
            final ExecutionContext context) {

        context.getLogger().info("Processing a request to generate OTP for Payment.");

        if (!isValidRequest(request.getBody())) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Missing request body").build();
        }

        OTPRequest receivedRequest = request.getBody();

        try {
            OrangeMoneyApi OMapi = new OrangeMoneyApi();

            // Assuming getPublicKey() and encryptPin() methods are correctly implemented and accessible
            OrangeUtils.PublicKeyResponse publicKey = OMapi.getPublicKey();
            String encryptedKey = OrangeUtils.encryptPin(receivedRequest.getPinCode(), publicKey.getKey());

            GenerateOTPRequest gOtpRequest = new GenerateOTPRequest();            
            gOtpRequest.setEncryptedPinCode(encryptedKey);
            gOtpRequest.setWalletType("PRINCIPAL");
            gOtpRequest.setId(receivedRequest.getPhoneNumber());
            gOtpRequest.setIdType("MSISDN");

            ObjectMapper mapper = new ObjectMapper();
            String jsonRequest = mapper.writeValueAsString(gOtpRequest);
            String otpResponse = OMapi.callOrangeMoneyApi("api/eWallet/v1/payments/otp", jsonRequest);

            
         // Parse the JSON response
            JsonNode rootNode = mapper.readTree(otpResponse);
            String otp = rootNode.path("otp").asText(); 

            return request.createResponseBuilder(HttpStatus.OK).body(otp).build();
        } catch (Exception e) {
            context.getLogger().severe("Error processing payment: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing payment: " + e.getMessage()).build();
        }
    }

    private boolean isValidRequest(OTPRequest request) {
        // Check encryptedPinCode length and Base64 encoding
        if (request.getPinCode() == null ||
                request.getPinCode().length() != 344 ||
                !isBase64Encoded(request.getPinCode())) {
            return false;
        }

        // Check id pattern
        if (request.getPhoneNumber() == null || !request.getPhoneNumber().matches("^\\d{9}$")) {
            return false;
        }

        return true; // All checks passed
    }

    private boolean isBase64Encoded(String value) {
        if (value == null) {
            return false;
        }
        try {
            java.util.Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

}
