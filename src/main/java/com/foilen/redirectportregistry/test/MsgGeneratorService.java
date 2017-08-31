/*
    Redirectport-Registry Test
    https://github.com/foilen/redirectport-registry-test
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.test;

import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foilen.smalltools.TimeoutHandler;
import com.foilen.smalltools.TimeoutHandler.TimeoutHandlerRunnable;
import com.foilen.smalltools.exception.SmallToolsException;
import com.foilen.smalltools.hash.HashMd5sum;
import com.foilen.smalltools.tools.AssertTools;
import com.foilen.smalltools.tools.CloseableTools;
import com.foilen.smalltools.tools.ThreadTools;

public class MsgGeneratorService extends Thread {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final static Logger logger = LoggerFactory.getLogger(MsgGeneratorService.class);

    private int rawIn;
    private int payloadSizeBytes;
    private int delaySec;

    public MsgGeneratorService(int rawIn, int payloadSizeBytes, int delaySec) {
        this.rawIn = rawIn;
        this.payloadSizeBytes = payloadSizeBytes;
        this.delaySec = delaySec;
        start();
    }

    @Override
    public void run() {

        ThreadTools.nameThread() //
                .clear() //
                .setSeparator(" - ") //
                .appendText("Msg Generator") //
                .appendText("payloadSizeBytes " + payloadSizeBytes) //
                .appendText("delaySec " + delaySec) //
                .change();

        ThreadTools.sleep(5 * 1000l);

        logger.info("Will start sending messages of {} bytes to {} with {} src delay", payloadSizeBytes, rawIn, delaySec);

        byte[] msg = new byte[payloadSizeBytes];
        Socket socket = null;
        while (true) {

            try {

                if (socket == null) {
                    logger.info("Creating a connection");
                    socket = new Socket("127.0.0.1", rawIn);
                }

                logger.info("Sending a message");
                SECURE_RANDOM.nextBytes(msg);
                socket.getOutputStream().write(msg);
                socket.getOutputStream().flush();
                String expectedHash = HashMd5sum.hashBytes(msg);
                for (int i = 0; i < payloadSizeBytes; ++i) {
                    msg[i] = 0;
                }

                logger.debug("Waiting for the message to come back");
                Socket currentSocket = socket;
                TimeoutHandler<Void> timeoutHandler = new TimeoutHandler<>(10000, new TimeoutHandlerRunnable<Void>() {

                    private RuntimeException thrown;

                    @Override
                    public Void result() {
                        if (thrown != null) {
                            throw thrown;
                        }
                        return null;
                    }

                    @Override
                    public void run() {
                        try {
                            int read = 0;

                            while (read < payloadSizeBytes) {
                                int len = currentSocket.getInputStream().read(msg, read, payloadSizeBytes - read);
                                if (len == -1) {
                                    throw new SmallToolsException("End of file");
                                }
                                read += len;
                                if (read >= payloadSizeBytes) {
                                    return;
                                }
                            }
                        } catch (IOException e) {
                            thrown = new SmallToolsException("Problem reading the message", e);
                        }

                    }

                    @Override
                    public void stopRequested() {
                        CloseableTools.close(currentSocket);
                    }
                });
                timeoutHandler.call();

                // Check the received message
                String actualHash = HashMd5sum.hashBytes(msg);
                logger.debug("Expected msg hash {} ; actual msg hash {}", expectedHash, actualHash);
                AssertTools.assertTrue(expectedHash.equals(actualHash), "The received message is not the one sent");

                logger.debug("Waiting {} seconds", delaySec);
                ThreadTools.sleep(delaySec * 1000l);

            } catch (Exception e) {
                logger.error("Got an exception. Will wait 10 seconds", e);
                CloseableTools.close(socket);
                socket = null;
                ThreadTools.sleep(10 * 1000l);
            }
        }

    }
}
