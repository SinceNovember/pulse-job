package com.simple.pulsejob.admin.scheduler.dispatch;

import java.util.List;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class BroadcastDispatcher extends AbstractDispatcher{
    @Override
    public void dispatch(JRequest request) {
        final Serializer _serializer = serializer();
        final MessageWrapper message = request.getMessage();

        ExecutorKey executorKey = request.getExecutor();

        JChannelGroup channelGroup = channelGroup(executorKey);
        List<JChannel> channels = channelGroup.channels();
        byte s_code = _serializer.code();
        for (JChannel channel : channels) {
            OutputBuf outputBuf =
                _serializer.writeObject(channel.allocOutputBuf(), message);
            request.outputBuf(s_code, JProtocolHeader.TRIGGER_JOB, outputBuf);
            write(channel, request, DispatchType.BROADCAST);
        }
    }
}
