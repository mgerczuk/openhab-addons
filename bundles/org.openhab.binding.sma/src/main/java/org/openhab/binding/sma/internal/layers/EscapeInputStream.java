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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class EscapeInputStream extends FilterInputStream {

    private boolean isEof = false;

    protected EscapeInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {

        if (isEof) {
            return -1;
        }

        int c = super.read();
        if (c < 0) {
            throw new IOException("EOF");
        }
        if (c == PPPFrame.HDLC_SYNC) {
            isEof = true;
            return -1;
        }
        if (c == PPPFrame.HDLC_ESC) {
            return super.read() ^ 0x20;
        }
        return c;
    }
}
