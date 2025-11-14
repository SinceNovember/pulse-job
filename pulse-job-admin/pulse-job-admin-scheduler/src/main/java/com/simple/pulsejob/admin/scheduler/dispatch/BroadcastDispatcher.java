package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class BroadcastDispatcher extends AbstractDispatcher{
    @Override
    public void dispatch(JRequest request) {
        final Serializer _serializer = serializer();
        final MessageWrapper message = request.getMessage();

        JChannelGroup[] groups = groups(request.getExecutor());
        JChannel[] channels = new JChannel[groups.length];
        for (int i = 0; i < groups.length; i++) {
            channels[i] = groups[i].next();
        }
        byte s_code = _serializer.code();
        for (JChannel channel : channels) {
            OutputBuf outputBuf =
                _serializer.writeObject(channel.allocOutputBuf(), message);
            request.outputBuf(s_code, JProtocolHeader.TRIGGER_JOB, outputBuf);
            write(channel, request, DispatchType.BROADCAST);
        }
    }
}
