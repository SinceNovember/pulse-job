package com.simple.pulsejob.serialization.java;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.InputBuf;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.plusejob.serialization.io.OutputStreams;
import com.simple.pulsejob.common.util.ThrowUtil;
import com.simple.pulsejob.serialization.java.io.Inputs;
import com.simple.pulsejob.serialization.java.io.Outputs;

import java.io.*;

public class JavaSerializer extends Serializer {
    @Override
    public SerializerType code() {
        return SerializerType.JAVA;
    }

    @Override
    public <T> OutputBuf writeObject(OutputBuf outputBuf, T obj) {
        try (ObjectOutputStream output = Outputs.getOutput(outputBuf)){
            output.writeObject(obj);
            output.flush();
            return outputBuf;
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        }
        return null;
    }

    @Override
    public <T> byte[] writeObject(T obj) {
        ByteArrayOutputStream buf = OutputStreams.getByteArrayOutputStream();
        try (ObjectOutputStream output = Outputs.getOutput(buf)) {
            output.writeObject(obj);
            output.flush();
            return buf.toByteArray();
        } catch (IOException e) {
            ThrowUtil.throwException(e);
        }
        return null;
    }

    @Override
    public <T> T readObject(InputBuf inputBuf, Class<T> clazz) {
        try (ObjectInputStream input = Inputs.getInput(inputBuf)) {
            Object obj = input.readObject();
            return clazz.cast(obj);
        } catch (Exception e) {
            ThrowUtil.throwException(e);
        } finally {
            inputBuf.release();
        }
        return null;
    }

    @Override
    public <T> T readObject(byte[] bytes, int offset, int length, Class<T> clazz) {
        try (ObjectInputStream input = Inputs.getInput(bytes, offset, length)) {
            Object obj = input.readObject();
            return clazz.cast(obj);
        } catch (Exception e) {
            ThrowUtil.throwException(e);
        }
        return null; // never get here
    }

    @Override
    public String toString() {
        return "java:(code=" + code() + ")";
    }
}
