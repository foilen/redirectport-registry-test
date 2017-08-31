/*
    Redirectport-Registry Test
    https://github.com/foilen/redirectport-registry-test
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.test;

import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.foilen.smalltools.net.services.SocketCallback;
import com.foilen.smalltools.tools.CloseableTools;
import com.foilen.smalltools.tools.StreamsTools;
import com.foilen.smalltools.tools.ThreadTools;

public class EchoSocketCallback implements SocketCallback {

    private final static Logger logger = LoggerFactory.getLogger(RedirectPortRegistryTestApp.class);

    @Override
    public void newClient(Socket socket) {

        Thread thread = new Thread(() -> {

            ThreadTools.nameThread() //
                    .clear() //
                    .setSeparator(" - ") //
                    .appendDate() //
                    .appendObjectText(socket.getLocalPort()) //
                    .change();

            logger.info("Connection");

            OutputStream outputStream = null;
            try {
                outputStream = socket.getOutputStream();
                StreamsTools.flowStream(socket.getInputStream(), outputStream);
            } catch (Exception e) {
                logger.error("Problem while transfering data", e);
                return;
            } finally {
                CloseableTools.close(outputStream);
            }

            logger.info("Disconnection");
        });
        thread.setDaemon(true);
        thread.start();

    }

}
