/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

public class DataHeader extends SMAPPPFrame {
    private long n1;
    private long n2;

    @Override
    public void read(BinaryInputStream rd) throws IOException {
        super.read(rd);

        assert rd.tell() == 24;
        long unkn3 = rd.readUInt(); // 24
        n1 = rd.readUInt(); // 28
        n2 = rd.readUInt(); // 32

        logger.debug("              {}", String.format("0x%08X", unkn3));
        logger.debug("  c         = {}", n1);
        logger.debug("  b         = {}", n2);
    }

    public int getRecordSize() {
        return 4 * (getLengthDWords() - 9) / (int) (n2 - n1 + 1);
    }

}
