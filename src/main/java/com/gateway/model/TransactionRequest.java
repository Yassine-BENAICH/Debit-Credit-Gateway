package com.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{16,19}$", message = "Invalid card number")
    private String cardNumber;
    
    @NotBlank(message = "Transaction type is required")
    @Pattern(regexp = "^(DEBIT|CREDIT|REFUND|REVERSAL)$", message = "Invalid transaction type")
    private String transactionType;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999.99", message = "Amount exceeds maximum")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency code is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Invalid currency code")
    private String currencyCode;
    
    @NotBlank(message = "Terminal ID is required")
    @Size(min = 8, max = 8, message = "Terminal ID must be 8 characters")
    private String terminalId;
    
    @NotBlank(message = "Merchant ID is required")
    @Size(min = 15, max = 15, message = "Merchant ID must be 15 characters")
    private String merchantId;
    
    @NotBlank(message = "Merchant name is required")
    @Size(max = 40, message = "Merchant name too long")
    private String merchantName;
    
    @NotBlank(message = "POS entry mode is required")
    @Pattern(regexp = "^[0-9]{2}$", message = "Invalid POS entry mode")
    private String posEntryMode;
    
    @Pattern(regexp = "^[0-9]{3}$", message = "Invalid merchant category code")
    private String merchantCategoryCode;
    
    private String cardExpiryDate;
    private String cvv;
    
    @Size(max = 12, message = "Invoice number too long")
    private String invoiceNumber;
    
    private String originalTransactionId;
    private String originalRRN;
    
    // Additional fields for specific transaction types
    private String fromAccount;
    private String toAccount;
    private String description;
}