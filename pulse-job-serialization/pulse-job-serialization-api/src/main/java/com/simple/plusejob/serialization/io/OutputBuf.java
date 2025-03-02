package com.simple.plusejob.serialization.io;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public interface OutputBuf {

    OutputStream outputStream();

    ByteBuffer nioByteBuffer(int minWritableBytes);

    int size();

    boolean hasMemoryAddress();

    Object backingObject();
}
