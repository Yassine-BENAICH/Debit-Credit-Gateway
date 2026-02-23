package com.gateway.model.enums;

import lombok.Getter;

@Getter
public enum ResponseCode {
    APPROVED("00", "Approved"),
    REFER_TO_ISSUER("01", "Refer to issuer"),
    DO_NOT_HONOR("05", "Do not honor"),
    INVALID_TRANSACTION("12", "Invalid transaction"),
    INVALID_AMOUNT("13", "Invalid amount"),
    INVALID_CARD("14", "Invalid card number"),
    NO_ISSUER("15", "No such issuer"),
    FORMAT_ERROR("30", "Format error"),
    LOST_CARD("41", "Lost card"),
    STOLEN_CARD("43", "Stolen card"),
    INSUFFICIENT_FUNDS("51", "Insufficient funds"),
    EXPIRED_CARD("54", "Expired card"),
    INVALID_PIN("55", "Invalid PIN"),
    TRANSACTION_NOT_PERMITTED("57", "Transaction not permitted"),
    SYSTEM_ERROR("96", "System error"),
    TIMEOUT("97", "Timeout"),
    DUPLICATE("94", "Duplicate transaction");
    
    private final String code;
    private final String message;
    
    ResponseCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public static ResponseCode fromCode(String code) {
        for (ResponseCode rc : values()) {
            if (rc.code.equals(code)) {
                return rc;
            }
        }
        return SYSTEM_ERROR;
    }
}