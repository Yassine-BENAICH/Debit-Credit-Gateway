package com.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Configuration
@ConfigurationProperties(prefix = "gateway.tcp")
@SuppressWarnings("unused")
public class TcpConfig {
    private String host = "localhost";
    private int port = 5000;
    private int connectionTimeout = 30000;
    private int readTimeout = 30000;
    private int maxConnections = 10;
    private int minConnections = 2;
    private boolean keepAlive = true;
    private boolean tcpNoDelay = true;
    private int retryAttempts = 3;
    private int retryDelay = 1000;
    private int maxRetryDelay = 10000;
    private int connectionPoolTimeout = 5000;
    private boolean lengthHeader = true;
    private int lengthHeaderSize = 2;
    private String encoding = "UTF-8";

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMinConnections() {
        return minConnections;
    }

    public void setMinConnections(int minConnections) {
        this.minConnections = minConnections;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public void setTcpNoDelay(boolean tcpNoDelay) {
        this.tcpNoDelay = tcpNoDelay;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(int retryDelay) {
        this.retryDelay = retryDelay;
    }

    public int getMaxRetryDelay() {
        return maxRetryDelay;
    }

    public void setMaxRetryDelay(int maxRetryDelay) {
        this.maxRetryDelay = maxRetryDelay;
    }

    public int getConnectionPoolTimeout() {
        return connectionPoolTimeout;
    }

    public void setConnectionPoolTimeout(int connectionPoolTimeout) {
        this.connectionPoolTimeout = connectionPoolTimeout;
    }

    public boolean isLengthHeader() {
        return lengthHeader;
    }

    public void setLengthHeader(boolean lengthHeader) {
        this.lengthHeader = lengthHeader;
    }

    public int getLengthHeaderSize() {
        return lengthHeaderSize;
    }

    public void setLengthHeaderSize(int lengthHeaderSize) {
        this.lengthHeaderSize = lengthHeaderSize;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }
}