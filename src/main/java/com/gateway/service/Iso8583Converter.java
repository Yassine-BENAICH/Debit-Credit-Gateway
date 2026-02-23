package com.gateway.service;

import com.gateway.iso8583.CustomPackager;
import com.gateway.model.TransactionRequest;
import com.gateway.model.TransactionResponse;
import com.gateway.model.enums.ResponseCode;
import com.gateway.model.enums.TransactionType;
import com.gateway.util.Iso8583Util;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@Log4j2
public class Iso8583Converter {
    
    private final CustomPackager customPackager;
    private final Iso8583Util iso8583Util;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public ISOMsg requestToIso(TransactionRequest request) throws ISOException {
        TransactionType transactionType = TransactionType.valueOf(request.getTransactionType());
        String mti = getMtiForTransactionType(transactionType);
        
        ISOMsg isoMsg = customPackager.createMessage(mti);
        LocalDateTime now = LocalDateTime.now();
        String stan = iso8583Util.generateStan();
        String rrn = iso8583Util.generateRRN();
        
        // Field 2 - Primary Account Number (PAN)
        isoMsg.set(2, request.getCardNumber());
        
        // Field 3 - Processing Code
        isoMsg.set(3, getProcessingCode(request));
        
        // Field 4 - Transaction Amount
        isoMsg.set(4, iso8583Util.formatAmount(request.getAmount()));
        
        // Field 7 - Transmission Date & Time
        isoMsg.set(7, iso8583Util.formatDateTime(now));
        
        // Field 11 - System Trace Audit Number (STAN)
        isoMsg.set(11, stan);
        
        // Field 12 - Local Transaction Time
        isoMsg.set(12, now.format(TIME_FORMAT));
        
        // Field 13 - Local Transaction Date
        isoMsg.set(13, now.format(DATE_FORMAT));
        
        // Field 14 - Card Expiry Date
        if (request.getCardExpiryDate() != null) {
            isoMsg.set(14, request.getCardExpiryDate());
        }
        
        // Field 18 - Merchant Category Code
        if (request.getMerchantCategoryCode() != null) {
            isoMsg.set(18, request.getMerchantCategoryCode());
        }
        
        // Field 22 - POS Entry Mode
        isoMsg.set(22, request.getPosEntryMode());
        
        // Field 25 - POS Condition Code
        isoMsg.set(25, "00"); // Normal presentation
        
        // Field 26 - POS PIN Capture Code
        isoMsg.set(26, "12"); // PIN capture capability
        
        // Field 32 - Acquiring Institution ID
        isoMsg.set(32, "123456");
        
        // Field 35 - Track 2 Data
        if (request.getCardExpiryDate() != null) {
            String track2 = request.getCardNumber() + "=" + request.getCardExpiryDate();
            isoMsg.set(35, track2);
        }
        
        // Field 37 - Retrieval Reference Number (RRN)
        isoMsg.set(37, rrn);
        
        // Field 41 - Terminal ID
        isoMsg.set(41, request.getTerminalId());
        
        // Field 42 - Merchant ID
        isoMsg.set(42, request.getMerchantId());
        
        // Field 43 - Merchant Name & Location
        isoMsg.set(43, formatMerchantName(request));
        
        // Field 49 - Currency Code
        isoMsg.set(49, request.getCurrencyCode());
        
        // Field 60 - Additional Data (Invoice number, etc.)
        if (request.getInvoiceNumber() != null) {
            isoMsg.set(60, "INV" + request.getInvoiceNumber());
        }
        
        // Field 62 - Additional Transaction Data
        StringBuilder additionalData = new StringBuilder();
        if (request.getDescription() != null) {
            additionalData.append(request.getDescription());
        }
        if (additionalData.length() > 0) {
            isoMsg.set(62, additionalData.toString());
        }
        
        // Field 102 - From Account
        if (request.getFromAccount() != null) {
            isoMsg.set(102, request.getFromAccount());
        }
        
        // Field 103 - To Account
        if (request.getToAccount() != null) {
            isoMsg.set(103, request.getToAccount());
        }
        
        log.info("Created ISO message: MTI={}, STAN={}, RRN={}", mti, stan, rrn);
        iso8583Util.logISOMsg(isoMsg);
        
        return isoMsg;
    }
    
    public TransactionResponse isoToResponse(ISOMsg isoMsg) throws ISOException {
        String responseCode = isoMsg.hasField(39) ? isoMsg.getString(39) : "96";
        ResponseCode rc = ResponseCode.fromCode(responseCode);
        
        TransactionResponse response = TransactionResponse.builder()
            .transactionId(UUID.randomUUID().toString())
            .rrn(isoMsg.hasField(37) ? isoMsg.getString(37) : null)
            .stan(isoMsg.hasField(11) ? isoMsg.getString(11) : null)
            .authCode(isoMsg.hasField(38) ? isoMsg.getString(38) : generateAuthCode())
            .responseCode(responseCode)
            .responseMessage(rc.getMessage())
            .approved("00".equals(responseCode))
            .status("00".equals(responseCode) ? "SUCCESS" : "FAILED")
            .processingTime(System.currentTimeMillis())
            .hostResponseCode(responseCode)
            .build();
        
        // Optional fields
        if (isoMsg.hasField(2)) {
            response.setCardNumber(isoMsg.getString(2));
        }
        
        if (isoMsg.hasField(4)) {
            response.setAmount(iso8583Util.parseAmount(isoMsg.getString(4)));
        }
        
        if (isoMsg.hasField(41)) {
            response.setTerminalId(isoMsg.getString(41));
        }
        
        if (isoMsg.hasField(42)) {
            response.setMerchantId(isoMsg.getString(42));
        }
        
        if (isoMsg.hasField(49)) {
            response.setCurrencyCode(isoMsg.getString(49));
        }
        
        if (isoMsg.hasField(7)) {
            // Could parse and set transaction date
        }
        
        log.info("Converted ISO response: Code={}, Message={}", responseCode, rc.getMessage());
        
        return response;
    }
    
    public ISOMsg createReversalMessage(TransactionRequest request, String originalRRN, String originalSTAN) throws ISOException {
        ISOMsg isoMsg = customPackager.createMessage("0400");
        LocalDateTime now = LocalDateTime.now();
        
        // Copy original transaction details
        isoMsg.set(2, request.getCardNumber());
        isoMsg.set(3, getProcessingCode(request));
        isoMsg.set(4, iso8583Util.formatAmount(request.getAmount()));
        isoMsg.set(11, originalSTAN); // Original STAN
        isoMsg.set(12, now.format(TIME_FORMAT));
        isoMsg.set(13, now.format(DATE_FORMAT));
        isoMsg.set(37, originalRRN); // Original RRN
        isoMsg.set(41, request.getTerminalId());
        isoMsg.set(42, request.getMerchantId());
        isoMsg.set(49, request.getCurrencyCode());
        
        // Field 90 - Original Data Elements
        String originalData = originalSTAN + now.format(DATE_FORMAT) + now.format(TIME_FORMAT);
        isoMsg.set(90, originalData);
        
        return isoMsg;
    }
    
    private String getMtiForTransactionType(TransactionType type) {
        switch (type) {
            case DEBIT:
            case CREDIT:
                return "0200";
            case REVERSAL:
                return "0400";
            case BALANCE:
                return "0100";
            case PRE_AUTHORIZATION:
                return "0100";
            case COMPLETION:
                return "0220";
            default:
                return "0200";
        }
    }
    
    private String getProcessingCode(TransactionRequest request) {
        TransactionType type = TransactionType.valueOf(request.getTransactionType());
        
        String fromAccountType = "00"; // Default
        String toAccountType = "00"; // Default
        
        if (request.getFromAccount() != null) {
            fromAccountType = determineAccountType(request.getFromAccount());
        }
        
        if (request.getToAccount() != null) {
            toAccountType = determineAccountType(request.getToAccount());
        }
        
        return type.getProcessingCode() + fromAccountType + toAccountType;
    }
    
    private String determineAccountType(String account) {
        if (account.startsWith("4")) {
            return "10"; // Savings account
        } else if (account.startsWith("5")) {
            return "20"; // Checking account
        } else {
            return "00"; // Default
        }
    }
    
    private String formatMerchantName(TransactionRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getMerchantName() != null) {
            sb.append(request.getMerchantName());
        }
        // Pad or truncate to 40 characters
        String result = sb.toString();
        if (result.length() > 40) {
            result = result.substring(0, 40);
        } else if (result.length() < 40) {
            result = String.format("%-40s", result);
        }
        return result;
    }
    
    private String generateAuthCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}