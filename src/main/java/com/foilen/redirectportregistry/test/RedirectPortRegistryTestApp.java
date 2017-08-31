/*
    Redirectport-Registry Test
    https://github.com/foilen/redirectport-registry-test
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.foilen.redirectportregistry.model.RedirectPortRegistryEntries;
import com.foilen.redirectportregistry.model.RedirectPortRegistryEntry;
import com.foilen.redirectportregistry.model.RedirectPortRegistryExit;
import com.foilen.redirectportregistry.model.RedirectPortRegistryExits;
import com.foilen.smalltools.consolerunner.ConsoleRunner;
import com.foilen.smalltools.crypt.asymmetric.AsymmetricKeys;
import com.foilen.smalltools.crypt.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.cert.CertificateDetails;
import com.foilen.smalltools.crypt.cert.RSACertificate;
import com.foilen.smalltools.net.services.TCPServerService;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.LogbackTools;

public class RedirectPortRegistryTestApp {

    private static final String LOCALHOST = "127.0.0.1";
    private static final String ENDPOINT = "endpoint";
    private static final String SERVICE_NAME = "serviceName";

    // TODO +++ Need refresh
    public static void main(String[] args) throws IOException {

        // Get the parameters
        RedirectPortRegistryTestOptions redirectPortRegistryTestOptions = new RedirectPortRegistryTestOptions();
        CmdLineParser cmdLineParser = new CmdLineParser(redirectPortRegistryTestOptions);
        try {
            cmdLineParser.parseArgument(args);
        } catch (CmdLineException e) {
            e.printStackTrace();
            showUsage();
            return;
        }

        // Check if debug
        if (redirectPortRegistryTestOptions.debug) {
            LogbackTools.changeConfig("/logback-debug.xml");
        } else {
            LogbackTools.changeConfig("/logback-normal.xml");
        }

        List<String> inArguments = new ArrayList<>();
        List<String> outArguments = new ArrayList<>();

        // Debug
        if (redirectPortRegistryTestOptions.debug) {
            inArguments.add("--debug");
            inArguments.add("true");
            outArguments.add("--debug");
            outArguments.add("true");
        }

        // Bridge port
        int bridgePort = (int) (Math.random() * 50000 + 10000);
        outArguments.add("--bridgePort");
        outArguments.add(String.valueOf(bridgePort));

        // Encryption needed
        if (redirectPortRegistryTestOptions.encrypted) {
            // Create the self signed certificate
            RSACrypt rsaCrypt = new RSACrypt();
            AsymmetricKeys keys = rsaCrypt.generateKeyPair(2048);
            RSACertificate certificate = new RSACertificate(keys);
            certificate.selfSign(new CertificateDetails().setCommonName("CA root"));

            // Save the files
            String caCertsFile = File.createTempFile("caCertsFile", ".json").getAbsolutePath();
            String certFile = File.createTempFile("certFile", ".pem").getAbsolutePath();
            String privateKeyFile = File.createTempFile("privateKeyFile", ".pem").getAbsolutePath();

            FileTools.writeFile(JsonTools.compactPrint(Collections.singleton(certificate.saveCertificatePemAsString())), caCertsFile);
            certificate.saveCertificatePem(certFile);
            rsaCrypt.savePrivateKeyPem(keys, privateKeyFile);

            // Add the arguments
            inArguments.add("--caCertsFile");
            inArguments.add(caCertsFile);
            inArguments.add("--bridgeCertFile");
            inArguments.add(certFile);
            inArguments.add("--bridgePrivateKeyFile");
            inArguments.add(privateKeyFile);
            outArguments.add("--caCertsFile");
            outArguments.add(caCertsFile);
            outArguments.add("--bridgeCertFile");
            outArguments.add(certFile);
            outArguments.add("--bridgePrivateKeyFile");
            outArguments.add(privateKeyFile);
        }

        // Start the Echo service
        @SuppressWarnings("resource")
        TCPServerService echoService = new TCPServerService(new EchoSocketCallback());

        // Create in registry
        int rawIn = (int) (Math.random() * 50000 + 10000);
        String inRegistryFile = File.createTempFile("inRegistry", ".json").getAbsolutePath();
        inArguments.add("--entryBridgeRegistryFile");
        inArguments.add(inRegistryFile);
        RedirectPortRegistryEntries redirectPortRegistryEntries = new RedirectPortRegistryEntries();
        redirectPortRegistryEntries.getEntries().add(new RedirectPortRegistryEntry(rawIn, LOCALHOST, bridgePort, SERVICE_NAME, ENDPOINT));
        JsonTools.writeToFile(inRegistryFile, redirectPortRegistryEntries);

        // Create out registry
        int rawOut = echoService.getPort();
        String outRegistryFile = File.createTempFile("outRegistry", ".json").getAbsolutePath();
        outArguments.add("--exitBridgeRegistryFile");
        outArguments.add(outRegistryFile);
        RedirectPortRegistryExits redirectPortRegistryExits = new RedirectPortRegistryExits();
        redirectPortRegistryExits.getExits().add(new RedirectPortRegistryExit(SERVICE_NAME, ENDPOINT, LOCALHOST, rawOut));
        JsonTools.writeToFile(outRegistryFile, redirectPortRegistryExits);

        // Start both services
        String redirectportCommand = "/app/bin/redirectport-registry";
        new Thread(() -> {
            ConsoleRunner consoleRunner = new ConsoleRunner();
            consoleRunner.setCommand(redirectportCommand);
            consoleRunner.addArguments(inArguments);
            consoleRunner.execute();
        }).start();
        new Thread(() -> {
            ConsoleRunner consoleRunner = new ConsoleRunner();
            consoleRunner.setCommand(redirectportCommand);
            consoleRunner.addArguments(outArguments);
            consoleRunner.execute();
        }).start();

        // Start message generator service
        new MsgGeneratorService(rawIn, redirectPortRegistryTestOptions.payloadSizeBytes, redirectPortRegistryTestOptions.delaySec);

    }

    private static void showUsage() {
        System.out.println("Usage:");
        CmdLineParser cmdLineParser = new CmdLineParser(new RedirectPortRegistryTestOptions());
        cmdLineParser.printUsage(System.out);
    }

}
