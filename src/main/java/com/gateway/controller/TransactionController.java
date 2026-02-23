package com.gateway.controller;

import com.gateway.model.TransactionRequest;
import com.gateway.model.TransactionResponse;
import com.gateway.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Log4j2
public class TransactionController {
    
    private final TransactionService transactionService;
    
    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> processTransaction(
            @Valid @RequestBody TransactionRequest request) {
        
        log.info("Received transaction request: Type={}, Terminal={}", 
            request.getTransactionType(), request.getTerminalId());
        
        TransactionResponse response = transactionService.processTransaction(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @PostMapping("/process/async")
    public CompletableFuture<ResponseEntity<TransactionResponse>> processTransactionAsync(
            @Valid @RequestBody TransactionRequest request) {
        
        return transactionService.processTransactionAsync(request)
            .thenApply(response -> {
                if (response.isSuccess()) {
                    return ResponseEntity.ok(response);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            });
    }
    
    @PostMapping("/reverse")
    public ResponseEntity<TransactionResponse> reverseTransaction(
            @RequestParam String rrn,
            @RequestParam String stan) {
        
        log.info("Received reversal request: RRN={}, STAN={}", rrn, stan);
        
        TransactionResponse response = transactionService.reverseTransaction(rrn, stan);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    @GetMapping("/status/{rrn}")
    public ResponseEntity<TransactionResponse> getTransactionStatus(@PathVariable String rrn) {
        TransactionResponse response = transactionService.getTransactionStatus(rrn);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Debit/Credit Gateway is running");
    }
}