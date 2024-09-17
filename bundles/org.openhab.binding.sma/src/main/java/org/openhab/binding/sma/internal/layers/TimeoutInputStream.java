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

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * InputStream with read timeout
 *
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
public class TimeoutInputStream extends InputStream {

    InputStream in;
    private long readTimeoutMillis = 15000;

    protected TimeoutInputStream(InputStream in, long readTimeoutMillis) {
        this.in = in;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    private final class ReadRunnable implements Runnable {
        private final byte @Nullable [] buf;
        private final int offset;
        private final int bufsize;
        private int result = -1;
        @Nullable
        private Exception ex = null;

        private ReadRunnable(byte @Nullable [] buf, int offset, int bufsize) {
            this.buf = buf;
            this.offset = offset;
            this.bufsize = bufsize;
        }

        @Override
        public void run() {
            try {
                result = in.read(buf, offset, bufsize);
            } catch (Exception e) {
                ex = e;
            }
        }
    }

    @Override
    public int read(byte @Nullable [] b, int off, int len) throws IOException {
        try {
            ReadRunnable runnable = new ReadRunnable(b, off, len);
            Thread t = new Thread(runnable);
            t.start();
            t.join(readTimeoutMillis);
            if (t.isAlive()) {
                t.interrupt();
                throw new IOException("Timeout reading socket");
            }

            if (runnable.ex != null) {
                throw new IOException(runnable.ex);
            }

            return runnable.result;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int read() throws IOException {
        byte[] buf = new byte[1];
        if (read(buf, 0, 1) < 1) {
            return -1;
        }
        return buf[0];
    }
}
