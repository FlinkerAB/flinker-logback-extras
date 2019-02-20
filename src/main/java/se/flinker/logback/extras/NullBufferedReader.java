package se.flinker.logback.extras;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

class NullBufferedReader extends BufferedReader {

    public NullBufferedReader() {
        super(new Reader() {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                return 0;
            }
            @Override
            public void close() throws IOException {
            }
        });
    }

    @Override
    public int read() throws IOException {
        return -1;
    }
}
