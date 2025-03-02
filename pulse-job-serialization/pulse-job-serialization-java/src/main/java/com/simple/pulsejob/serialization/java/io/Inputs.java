package com.simple.pulsejob.serialization.java.io;

import com.simple.plusejob.serialization.io.InputBuf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

public final class Inputs {
    public static ObjectInputStream getInput(InputBuf inputBuf) throws IOException {
        return new ObjectInputStream(inputBuf.inputStream());
    }

    public static ObjectInputStream getInput(byte[] bytes, int offset, int length) throws IOException {
        return new ObjectInputStream(new ByteArrayInputStream(bytes, offset, length));
    }

    private Inputs() {}
}
