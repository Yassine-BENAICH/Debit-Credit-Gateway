package com.gateway.service;

import com.gateway.model.TransactionRequest;
import com.gateway.model.TransactionResponse;
import com.gateway.model.enums.ResponseCode;
import com.gateway.tcp.IsoTcpClient;
import com.gateway.util.Iso8583Util;
import com.gateway.converter.Iso8583Converter;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jpos.iso.ISOMsg;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionService {

    private final IsoTcpClient iso8583TcpClient;
    private final Iso8583Converter iso8583Converter;
    private final Iso8583Util iso8583Util;

    // Cache for pending reversals
    private final Map<String, TransactionRequest> pendingReversals = new ConcurrentHashMap<>();

    @Retryable(value = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public TransactionResponse processTransaction(TransactionRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = iso8583Util.generateRequestId();

        log.info("Processing transaction [{}]: Type={}, Terminal={}, Amount={} {}",
                requestId, request.getTransactionType(), request.getTerminalId(),
                request.getAmount(), request.getCurrencyCode());

        try {
            // Validate request
            validateRequest(request);

            // Convert request to ISO message
            ISOMsg isoRequest = iso8583Converter.requestToIso(request);

            // Send to host
            ISOMsg isoResponse = iso8583TcpClient.sendRequest(isoRequest);

            // Convert response
            TransactionResponse response = iso8583Converter.isoToResponse(isoResponse);

            // Store for potential reversal
            if (request.getTransactionType() == TransactionType.DEBIT && response.isSuccess()) {
                pendingReversals.put(isoRequest.getString(37), request);
            }

            // Handle response
            if (!response.isSuccess()) {
                storeForReversal(request, isoRequest.getString(37), isoRequest.getString(11));
            }

            // Calculate processing time
            response.setProcessingTime(System.currentTimeMillis() - startTime);

            log.info("Transaction completed [{}]: Code={}, Time={}ms",
                    requestId, response.getResponseCode(), response.getProcessingTime());

            return response;

        } catch (Exception e) {
            log.error("Transaction failed [{}]: {}", requestId, e.getMessage(), e);
            return createErrorResponse(request, ResponseCode.SYSTEM_ERROR);
        }
    }

    public CompletableFuture<TransactionResponse> processTransactionAsync(TransactionRequest request) {
        return CompletableFuture.supplyAsync(() -> processTransaction(request));
    }

    public TransactionResponse reverseTransaction(String originalRRN, String originalSTAN) {
        log.info("Processing reversal for RRN={}, STAN={}", originalRRN, originalSTAN);

        TransactionRequest originalRequest = pendingReversals.get(originalRRN);

        if (originalRequest == null) {
            log.warn("Original transaction not found for reversal: RRN={}", originalRRN);
            return TransactionResponse.builder()
                    .responseCode("25")
                    .responseMessage("Original transaction not found")
                    .approved(false)
                    .build();
        }

        try {
            ISOMsg reversalMsg = iso8583Converter.createReversalMessage(
                    originalRequest, originalRRN, originalSTAN);

            ISOMsg response = iso8583TcpClient.sendRequest(reversalMsg);
            TransactionResponse reversalResponse = iso8583Converter.isoToResponse(response);

            if (reversalResponse.isSuccess()) {
                pendingReversals.remove(originalRRN);
                log.info("Reversal successful for RRN={}", originalRRN);
            }

            return reversalResponse;

        } catch (Exception e) {
            log.error("Reversal failed for RRN={}: {}", originalRRN, e.getMessage());
            return TransactionResponse.builder()
                    .responseCode("96")
                    .responseMessage("Reversal failed")
                    .approved(false)
                    .build();
        }
    }

    @Cacheable(value = "transactionStatus", key = "#rrn")
    public TransactionResponse getTransactionStatus(String rrn) {
        log.info("Checking status for transaction RRN={}", rrn);

        // In a real implementation, this would query a database
        return TransactionResponse.builder()
                .rrn(rrn)
                .responseCode("00")
                .responseMessage("Transaction found")
                .transactionDate(LocalDateTime.now())
                .build();
    }

    private void validateRequest(TransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid amount");
        }

        // Add more validation logic
    }

    private void storeForReversal(TransactionRequest request, String rrn, String stan) {
        // Store for 24 hours
        pendingReversals.put(rrn, request);
    }

    private TransactionResponse createErrorResponse(TransactionRequest request, ResponseCode responseCode) {
        return TransactionResponse.builder()
                .transactionId(iso8583Util.generateRequestId())
                .responseCode(responseCode.getCode())
                .responseMessage(responseCode.getMessage())
                .amount(request.getAmount())
                .currencyCode(request.getCurrencyCode())
                .terminalId(request.getTerminalId())
                .merchantId(request.getMerchantId())
                .cardNumber(request.getCardNumber())
                .maskedCardNumber(maskCardNumber(request.getCardNumber()))
                .transactionType(request.getTransactionType())
                .transactionDate(LocalDateTime.now())
                .approved(false)
                .status("FAILED")
                .processingTime(System.currentTimeMillis())
                .build();
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 10)
            return cardNumber;
        return cardNumber.substring(0, 6) + "******" + cardNumber.substring(cardNumber.length() - 4);
    }
}