package com.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateway.model.TransactionRequest;
import com.gateway.model.TransactionResponse;
import com.gateway.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockBean;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(TransactionController.class)
public class TransactionControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TransactionService transactionService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private TransactionRequest validRequest;
    private TransactionResponse successResponse;
    
    @BeforeEach
    void setUp() {
        validRequest = TransactionRequest.builder()
            .cardNumber("4111111111111111")
            .transactionType("DEBIT")
            .amount(new BigDecimal("100.00"))
            .currencyCode("USD")
            .terminalId("12345678")
            .merchantId("876543210123456")
            .merchantName("Test Merchant")
            .posEntryMode("05")
            .build();
        
        successResponse = TransactionResponse.builder()
            .responseCode("00")
            .responseMessage("Approved")
            .approved(true)
            .build();
    }
    
    @Test
    void testProcessTransaction_Success() throws Exception {
        when(transactionService.processTransaction(any(TransactionRequest.class)))
            .thenReturn(successResponse);
        
        mockMvc.perform(post("/api/v1/transactions/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseCode").value("00"))
                .andExpect(jsonPath("$.approved").value(true));
    }
}