package com.simple.pulsejob.serialization.hessian.io;

import com.caucho.hessian.io.Hessian2Output;
import com.simple.plusejob.serialization.io.OutputBuf;

import java.io.OutputStream;

public final class Outputs {

    public static Hessian2Output getOutput(OutputBuf outputBuf) {
        return new Hessian2Output(outputBuf.outputStream());
    }

    public static Hessian2Output getOutput(OutputStream buf) {
        return new Hessian2Output(buf);
    }

    private Outputs() {}
}
