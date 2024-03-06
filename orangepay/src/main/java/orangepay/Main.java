package orangepay;

import com.fasterxml.jackson.databind.ObjectMapper;

import orangepay.OrangeUtils.*;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");

        try {

            // Ensuring the endpoint URL ends with a slash for proper concatenation
            String OM_BASE_URL = System.getenv("OM_BASE_URL");

            String OM_CLIENT_ID = System.getenv("ORANGE_MONEY_CLIENT_ID");
            String OM_CLIENT_SECRET = System.getenv("ORANGE_MONEY_CLIENT_SECRET");

            System.out.println("OM_BASE_URL: " + OM_BASE_URL);
            System.out.println("OM_CLIENT_ID: " + OM_CLIENT_ID);
System.out.println("OM_CLIENT_SECRET: " + OM_CLIENT_SECRET);

            OrangeMoneyApi OMapi= new OrangeMoneyApi();
            // Assuming getPublicKey() and encryptPin() methods are correctly implemented and accessible
            OrangeUtils.PublicKeyResponse publicKey = OMapi.getPublicKey();
            String encryptedKey = OrangeUtils.encryptPin("2021", publicKey.getKey());

            GenerateOTPRequest otpRequest = new GenerateOTPRequest();            
            otpRequest.setEncryptedPinCode(encryptedKey);
            otpRequest.setWalletType("PRINCIPAL");
            otpRequest.setId("786175702");
            otpRequest.setIdType("MSISDN");

            ObjectMapper mapper = new ObjectMapper(); // Jackson's ObjectMapper
            String jsonRequest = mapper.writeValueAsString(otpRequest);
            String otpResponse = OMapi.callOrangeMoneyApi("api/eWallet/v1/payments/otp", jsonRequest);
            
            System.out.println("OTP Response: " + otpResponse);
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        }
    }
}
