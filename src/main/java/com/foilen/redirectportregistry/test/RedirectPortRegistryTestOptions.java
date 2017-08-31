/*
    Redirectport-Registry Test
    https://github.com/foilen/redirectport-registry-test
    Copyright (c) 2017 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.redirectportregistry.test;

import org.kohsuke.args4j.Option;

public class RedirectPortRegistryTestOptions {

    @Option(name = "--debug", usage = "To log everything")
    public boolean debug;
    @Option(name = "--encrypted", usage = "To encrypt the connection")
    public boolean encrypted;

    @Option(name = "--delaySec", metaVar = "seconds", usage = "How many seconds to wait between messages")
    public int delaySec;

    @Option(name = "--payloadSizeBytes", metaVar = "bytes", usage = "The size of one message")
    public int payloadSizeBytes;

}
