package orangepay;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

import javax.crypto.Cipher;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Utility class containing all models and enums for Orange Money integration.
 */
public class OrangeUtils {

    /**
     * Represents a monetary amount.
     */
    public static class Money {
        public double amount;
        public String currency;
    }

    /**
     * Represents a merchant or payer with specific attributes.
     */
    public static class Merchant {
        public String id;
        public String name;
        // Add other merchant-specific fields here
    }

    /**
     * Extends Merchant to represent a payer in a transaction, including
     * customer-specific fields.
     */
    public static class MerchantPayer extends Merchant {
        public String otp; // Customer OTP
        public IdType idType;
        public WalletType walletType;
    }

    /**
     * Enum for identifying the type of ID provided.
     */
    public enum IdType {
        CODE, MSISDN
    }

    /**
     * Enum for specifying the wallet type.
     */
    public enum WalletType {
        INTERNATIONAL, PRINCIPAL
    }

    /**
     * Request object for initiating a one-step payment.
     */
    public static class OneStepPaymentRequest {
        public Money amount;
        public MerchantPayer customer;
        public Map<String, String> metadata;
        public Merchant partner;
        public String reference;
    }

    public class MoneyReq {
        public double amount; // Adjust the type if needed
        public String currency; // Assuming currency is part of the amount object, adjust as per actual API
    }

    public class GenerateQRCodeRequest {
        public OrangeUtils.MoneyReq amount;
        public String callbackCancelUrl;
        public String callbackSuccessUrl;
        public String code; // Ensure this matches the regex validation "\d{6}"
        public Map<String, String> metadata; // This map should contain no more than 10 entries
        public String name;
        public long validity;

        // You might want to add a method to validate the request object, including the
        // metadata size
        public boolean isValid() {
            if (metadata != null && metadata.size() > 10) {
                return false; // Invalid if more than 10 metadata entries
            }
            // Add other validation logic as necessary
            return true;
        }

        public String getCode() {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'getCode'");
        }
    }

    public static class GenerateOTPRequest {

        @JsonProperty("encryptedPinCode")
        private String encryptedPinCode; // Base64 encrypted pincode
    
        @JsonProperty("id")
        private String id; // Party id such as msisdn value
    
        @JsonProperty("idType")
        private String idType; // "CODE" or "MSISDN"
    
        @JsonProperty("walletType")
        private String walletType; // "INTERNATIONAL" or "PRINCIPAL", optional
    
        // Getter for encryptedPinCode
        public String getEncryptedPinCode() {
            return encryptedPinCode;
        }
    
        // Setter for encryptedPinCode
        public void setEncryptedPinCode(String encryptedPinCode) {
            this.encryptedPinCode = encryptedPinCode;
        }
    
        // Getter for id
        public String getId() {
            return id;
        }
    
        // Setter for id
        public void setId(String id) {
            this.id = id;
        }
    
        // Getter for idType
        public String getIdType() {
            return idType;
        }
    
        // Setter for idType
        public void setIdType(String idType) {
            this.idType = idType;
        }
    
        // Getter for walletType
        public String getWalletType() {
            return walletType;
        }
    
        // Setter for walletType, considering it's optional
        public void setWalletType(String walletType) {
            this.walletType = walletType;
        }
    }

    public static boolean isValidPaymentRequest(OneStepPaymentRequest request) {
        // Example: Validate MerchantPayer (customer) id and otp format
        MerchantPayer customer = request.customer;
        if (!customer.id.matches("\\d{9}")) {
            return false; // Invalid id
        }
        if (!customer.otp.matches("\\d{6}")) {
            return false; // Invalid otp
        }
        if (!(customer.idType.equals("CODE") || customer.idType.equals("MSISDN"))) {
            return false; // Invalid idType
        }
        // Optionally validate walletType if provided
        if (customer.walletType != null
                && !(customer.walletType.equals("INTERNATIONAL") || customer.walletType.equals("PRINCIPAL"))) {
            return false; // Invalid walletType
        }

        return true;
    }

    public static class PublicKeyResponse {
        private String keyId;
        private String keyType;
        private int keySize;
        private String key;

        // Getters and Setters
        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public String getKeyType() {
            return keyType;
        }

        public void setKeyType(String keyType) {
            this.keyType = keyType;
        }

        public int getKeySize() {
            return keySize;
        }

        public void setKeySize(int keySize) {
            this.keySize = keySize;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }


    public static String encryptPin(String pin, String publicKeyStr) {
            try {
                // Convert public key string to PublicKey object
                byte[] publicBytes = Base64.getDecoder().decode(publicKeyStr);
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicKey = keyFactory.generatePublic(keySpec);

                // Encrypt PIN
                Cipher cipher = Cipher.getInstance("RSA");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                byte[] encryptedBytes = cipher.doFinal(pin.getBytes());

                // Encode in Base64
                return Base64.getEncoder().encodeToString(encryptedBytes);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static class OTPRequest {
            private String pinCode;
            private String phoneNumber;
    
            public String getPinCode() {
                return pinCode;
            }
    
            public void setPinCode(String pinCode) {
                this.pinCode = pinCode;
            }
    
            public String getPhoneNumber() {
                return phoneNumber;
            }
    
            public void setPhoneNumber(String phoneNumber) {
                this.phoneNumber = phoneNumber;
            }
        }
}
