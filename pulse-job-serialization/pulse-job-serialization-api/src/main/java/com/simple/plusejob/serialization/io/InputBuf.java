package com.simple.plusejob.serialization.io;

import java.io.InputStream;
import java.nio.ByteBuffer;

public interface InputBuf {

    InputStream inputStream();

    ByteBuffer nioByteBuffer();

    int size();

    boolean hasMemoryAddress();

    boolean release();
}
