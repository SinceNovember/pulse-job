package com.simple.pulsejob.serialization.java.io;

import com.simple.plusejob.serialization.io.OutputBuf;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public final class Outputs {

    public static ObjectOutputStream getOutput(OutputBuf outputBuf) throws IOException {
        return getOutput(outputBuf.outputStream());
    }

    public static ObjectOutputStream getOutput(OutputStream buf) throws IOException {
        return new ObjectOutputStream(buf);
    }

    private Outputs() {}
}
