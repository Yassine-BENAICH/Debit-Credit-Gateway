package com.gateway.util;

import lombok.extern.log4j.Log4j2;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

@Component
@Log4j2
public class Iso8583Util {
    
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMddHHmmss");
    private final Random random = new Random();
    
    public String generateStan() {
        return String.format("%06d", random.nextInt(999999));
    }
    
    public String generateRRN() {
        long timestamp = System.currentTimeMillis() % 1000000000000L;
        return String.format("%012d", timestamp);
    }
    
    public String generateRequestId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
    
    public String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "000000000000";
        }
        String amountStr = amount.multiply(new BigDecimal("100"))
            .setScale(0).toString();
        return String.format("%012d", Long.parseLong(amountStr));
    }
    
    public BigDecimal parseAmount(String amountStr) {
        try {
            return new BigDecimal(amountStr).divide(new BigDecimal("100"));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    public String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMAT);
    }
    
    public void logISOMsg(ISOMsg isoMsg) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("\n=== ISO8583 Message ===\n");
            sb.append("MTI: ").append(isoMsg.getMTI()).append("\n");
            
            for (int i = 1; i <= isoMsg.getMaxField(); i++) {
                if (isoMsg.hasField(i)) {
                    sb.append(String.format("Field %2d (%s): %s\n", 
                        i, getFieldName(i), isoMsg.getString(i)));
                }
            }
            sb.append("=======================");
            
            log.debug(sb.toString());
        } catch (ISOException e) {
            log.error("Error logging ISO message: {}", e.getMessage());
        }
    }
    
    private String getFieldName(int field) {
        switch (field) {
            case 2: return "PAN";
            case 3: return "Processing Code";
            case 4: return "Amount";
            case 7: return "Transmission Date/Time";
            case 11: return "STAN";
            case 12: return "Local Time";
            case 13: return "Local Date";
            case 14: return "Expiry Date";
            case 18: return "Merchant Category";
            case 22: return "POS Entry Mode";
            case 25: return "POS Condition";
            case 32: return "Acquiring Institution";
            case 35: return "Track 2";
            case 37: return "RRN";
            case 38: return "Auth Code";
            case 39: return "Response Code";
            case 41: return "Terminal ID";
            case 42: return "Merchant ID";
            case 43: return "Merchant Name";
            case 49: return "Currency";
            case 60: return "Additional Data";
            case 62: return "Transaction Data";
            default: return "Field " + field;
        }
    }
}