/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sma.internal.layers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.microedition.io.StreamConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class StreamConnectionDebug implements StreamConnection {

    private static final Logger logger = LoggerFactory.getLogger(StreamConnectionDebug.class);

    ByteArrayOutputStream osRead = new ByteArrayOutputStream();
    ByteArrayOutputStream osWrite = new ByteArrayOutputStream();

    static class CheckOutputStream extends OutputStream {

        ByteArrayInputStream is;

        public CheckOutputStream(byte[] expected) {
            is = new ByteArrayInputStream(expected);
        }

        @Override
        public void write(int b) throws IOException {
            int nextb = is.read();
            if (nextb > 127) {
                nextb -= 256;
            }
            assertEquals(nextb, b);
        }
    }

    public void addReadData(byte[] data) {
        try {
            osRead.write(data);
        } catch (IOException e) {
            logger.error("addReadData", e);
        }
    }

    public void addWriteData(byte[] data) {
        try {
            osWrite.write(data);
        } catch (IOException e) {
            logger.error("addReadData", e);
        }
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return new ByteArrayInputStream(osRead.toByteArray());
    }

    @Override
    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    @Override
    public void close() throws IOException {
        osRead = new ByteArrayOutputStream();
        osWrite = new ByteArrayOutputStream();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return new CheckOutputStream(osWrite.toByteArray());
    }

    @Override
    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }
}
