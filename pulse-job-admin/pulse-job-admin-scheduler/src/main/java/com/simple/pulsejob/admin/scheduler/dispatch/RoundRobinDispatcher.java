package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class RoundRobinDispatcher extends AbstractDispatcher {

    @Override
    public void dispatch(JRequest request) {
        final Serializer _serializer = serializer();
        final MessageWrapper message = request.getMessage();
        ExecutorKey executorKey = ExecutorKey.of(message.getExecutorName());

        // 通过软负载均衡选择一个channel
        JChannel channel = select(executorKey);

        byte s_code = _serializer.code();

        OutputBuf outputBuf =
                _serializer.writeObject(channel.allocOutputBuf(), message);
        request.outputBuf(s_code, JProtocolHeader.TRIGGER_JOB, outputBuf);
        executeJob(channel, request, DispatchType.ROUND);
    }
}