package com.gateway.model.enums;

import lombok.Getter;

@Getter
public enum TransactionType {
    DEBIT("01", "Purchase", "00"),
    CREDIT("02", "Refund", "20"),
    BALANCE("03", "Balance Inquiry", "31"),
    REVERSAL("04", "Reversal", "00"),
    PRE_AUTHORIZATION("05", "Pre-Authorization", "03"),
    COMPLETION("06", "Completion", "00");
    
    private final String code;
    private final String description;
    private final String processingCode;
    
    TransactionType(String code, String description, String processingCode) {
        this.code = code;
        this.description = description;
        this.processingCode = processingCode;
    }
    
    public static TransactionType fromCode(String code) {
        for (TransactionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transaction type: " + code);
    }
}