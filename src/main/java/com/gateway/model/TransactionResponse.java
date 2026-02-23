package com.gateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    
    private String transactionId;
    private String rrn;
    private String stan;
    private String authCode;
    private String responseCode;
    private String responseMessage;
    private BigDecimal amount;
    private String currencyCode;
    private String terminalId;
    private String merchantId;
    private String cardNumber;
    private String maskedCardNumber;
    private String transactionType;
    private LocalDateTime transactionDate;
    private String approvalCode;
    private String cardType;
    private String issuerCountry;
    private String acquirerId;
    private String retrievalReferenceNumber;
    private Boolean approved;
    private String status;
    private Long processingTime;
    private String hostResponseCode;
    
    public String getMaskedCardNumber() {
        if (cardNumber != null && cardNumber.length() >= 16) {
            return cardNumber.substring(0, 6) + "******" + cardNumber.substring(12);
        }
        return cardNumber;
    }
    
    public boolean isSuccess() {
        return "00".equals(responseCode) || "000".equals(responseCode);
    }
}