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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Martin Gerczuk - Initial contribution
 */
public class EscapeOutputStream extends FilterOutputStream {

    public EscapeOutputStream(OutputStream out) {
        super(out);
    }

    @Override
    public void write(int v) throws IOException {

        if (v == PPPFrame.HDLC_ESC || v == PPPFrame.HDLC_SYNC || v == 0x11 || v == 0x12 || v == 0x13) {
            super.write(PPPFrame.HDLC_ESC);
            super.write(v ^ 0x20);
        } else {
            super.write(v);
        }
    }
}
