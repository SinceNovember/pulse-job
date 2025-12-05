package com.simple.pulsejob.serialization.hessian;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.InputBuf;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.plusejob.serialization.io.OutputStreams;
import com.simple.pulsejob.common.util.ThrowUtil;
import com.simple.pulsejob.serialization.hessian.io.Inputs;
import com.simple.pulsejob.serialization.hessian.io.Outputs;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HessianSerializer extends Serializer {
    @Override
    public SerializerType code() {
        return SerializerType.HESSIAN;
    }

    @Override
    public <T> OutputBuf writeObject(OutputBuf outputBuf, T obj) {
        Hessian2Output output = Outputs.getOutput(outputBuf);
        try {
            output.writeObject(obj);
            output.flush();
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        } finally {
            try {
                output.close();
            } catch (IOException ignored) {

            }
        }
        return null;
    }

    @Override
    public <T> byte[] writeObject(T obj) {
        ByteArrayOutputStream buf = OutputStreams.getByteArrayOutputStream();
        Hessian2Output output = Outputs.getOutput(buf);
        try {
            output.writeObject(obj);
            output.flush();
            return buf.toByteArray();
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        } finally {
            try {
                output.close();
            } catch (IOException ignored) {}
            OutputStreams.resetBuf(buf);
        }
        return null;
    }

    @Override
    public <T> T readObject(InputBuf inputBuf, Class<T> clazz) {
        Hessian2Input input = Inputs.getInput(inputBuf);
        try {
            Object obj = input.readObject(clazz);
            return clazz.cast(obj);
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {}

            inputBuf.release();
        }
        return null; // never get here
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        Hessian2Input input = Inputs.getInput(bytes, offset, length);
        try {
            Object obj = input.readObject(clazz);
            return clazz.cast(obj);
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        } finally {
            try {
                input.close();
            } catch (IOException ignored) {}
        }
        return null; // never get here
    }

    @Override
    public String toString() {
        return "hessian:(code=" + code() + ")";
    }
}
