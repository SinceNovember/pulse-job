package com.simple.pulsejob.serialization.hessian.io;

import com.caucho.hessian.io.Hessian2Input;
import com.simple.plusejob.serialization.io.InputBuf;

import java.io.ByteArrayInputStream;

public class Inputs {

    public static Hessian2Input getInput(InputBuf inputBuf) {
        return new Hessian2Input(inputBuf.inputStream());
    }

    public static Hessian2Input getInput(byte[] bytes, int offset, int length) {
        return new Hessian2Input(new ByteArrayInputStream(bytes, offset, length));
    }

    private Inputs() {}
}
