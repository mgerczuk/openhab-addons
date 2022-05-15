package org.openhab.binding.sma.internal.layers;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class EscapeInputStream extends FilterInputStream {

    protected EscapeInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int c = super.read();
        if (c == PPPFrame.HDLC_ESC) {
            return super.read() ^ 0x20;
        }
        return c;
    }

}
