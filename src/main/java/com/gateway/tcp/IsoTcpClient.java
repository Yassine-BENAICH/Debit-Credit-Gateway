package com.gateway.tcp;

import com.gateway.config.TcpConfig;
import com.gateway.iso8583.CustomPackager;
import com.gateway.util.Iso8583Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Log4j2
public class IsoTcpClient {

    private final TcpConfig tcpConfig;
    private final CustomPackager customPackager;
    private final Iso8583Util iso8583Util;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final BlockingQueue<Socket> connectionPool = new LinkedBlockingQueue<>();
    private final AtomicInteger totalConnections = new AtomicInteger(0);

    @PostConstruct
    public void init() {
        int minConnections = tcpConfig.getMinConnections();
        for (int i = 0; i < minConnections; i++) {
            try {
                Socket socket = createNewConnection();
                if (connectionPool.offer(socket)) {
                    totalConnections.incrementAndGet();
                } else {
                    closeConnection(socket);
                }
            } catch (IOException e) {
                log.warn("Error creating initial connection {}: {}", i + 1, e.getMessage());
                break;
            }
        }
    }

    public ISOMsg sendRequest(ISOMsg request) throws IOException, ISOException {
        int attempts = 0;
        int maxAttempts = Math.max(1, tcpConfig.getRetryAttempts());
        IOException lastException = null;

        while (attempts < maxAttempts) {
            Socket socket = null;
            long startTime = System.currentTimeMillis();
            attempts++;

            try {
                socket = getConnection();

                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                DataInputStream dis = new DataInputStream(socket.getInputStream());

                byte[] message = customPackager.pack(request);

                if (tcpConfig.isLengthHeader()) {
                    byte[] lengthHeader = new byte[tcpConfig.getLengthHeaderSize()];
                    lengthHeader[0] = (byte) ((message.length >> 8) & 0xFF);
                    lengthHeader[1] = (byte) (message.length & 0xFF);
                    dos.write(lengthHeader);
                }

                dos.write(message);
                dos.flush();

                log.debug("Sent message: {}", ISOUtil.hexString(message));

                byte[] responseLength = new byte[tcpConfig.getLengthHeaderSize()];
                dis.readFully(responseLength);

                int responseMsgLength = ((responseLength[0] & 0xFF) << 8) | (responseLength[1] & 0xFF);

                byte[] responseData = new byte[responseMsgLength];
                dis.readFully(responseData);

                log.debug("Received response: {}", ISOUtil.hexString(responseData));

                ISOMsg response = customPackager.unpack(responseData);

                long elapsed = System.currentTimeMillis() - startTime;
                log.info("Round trip time: {}ms (attempt {}/{})", elapsed, attempts, maxAttempts);

                return response;

            } catch (IOException e) {
                lastException = e;
                log.error("Communication error on attempt {}/{}: {}", attempts, maxAttempts, e.getMessage());
                if (socket != null) {
                    invalidateConnection(socket);
                    socket = null;
                }

                if (attempts >= maxAttempts) {
                    throw e;
                }

                long baseDelay = Math.max(0, tcpConfig.getRetryDelay());
                long maxDelay = Math.max(baseDelay, tcpConfig.getMaxRetryDelay());
                long delay = baseDelay * (1L << (attempts - 1));
                if (delay > maxDelay) {
                    delay = maxDelay;
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted during retry delay", ie);
                }
            } finally {
                if (socket != null && !socket.isClosed()) {
                    releaseConnection(socket);
                }
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        throw new IOException("Failed to send ISO8583 request");
    }

    public CompletableFuture<ISOMsg> sendRequestAsync(ISOMsg request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendRequest(request);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }

    private synchronized Socket getConnection() throws IOException {
        Socket socket = connectionPool.poll();

        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            return socket;
        }

        if (totalConnections.get() < tcpConfig.getMaxConnections()) {
            socket = createNewConnection();
            totalConnections.incrementAndGet();
            return socket;
        }

        try {
            long timeout = Math.max(0, tcpConfig.getConnectionPoolTimeout());
            socket = connectionPool.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for connection", e);
        }

        if (socket == null || socket.isClosed() || !socket.isConnected()) {
            throw new IOException("No available connection in pool");
        }

        return socket;
    }

    private Socket createNewConnection() throws IOException {
        Socket socket = new Socket();
        socket.setSoTimeout(tcpConfig.getReadTimeout());
        socket.setKeepAlive(tcpConfig.isKeepAlive());
        socket.setTcpNoDelay(tcpConfig.isTcpNoDelay());

        socket.connect(new InetSocketAddress(tcpConfig.getHost(), tcpConfig.getPort()),
                tcpConfig.getConnectionTimeout());

        log.info("Created new connection to {}:{}", tcpConfig.getHost(), tcpConfig.getPort());
        return socket;
    }

    private void releaseConnection(Socket socket) {
        if (socket != null && socket.isConnected() && !socket.isClosed()) {
            if (!connectionPool.offer(socket)) {
                closeConnection(socket);
            }
        }
    }

    private void invalidateConnection(Socket socket) {
        closeConnection(socket);
    }

    private void closeConnection(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            log.warn("Error closing socket: {}", e.getMessage());
        } finally {
            if (totalConnections.get() > 0) {
                totalConnections.decrementAndGet();
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down TCP client");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        connectionPool.forEach(this::closeConnection);
        connectionPool.clear();
    }
}
