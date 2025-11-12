package com.simple.pulsejob.admin.scheduler.dispatch;

import java.util.List;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.interceptor.JInterceptor;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDispatcher implements Dispatcher {

    private final ExecutorChannelGroupManager channelGroupManager;

    private final List<JInterceptor> interceptors;

    @Override
    public void dispatch(JRequest request) {

    }

    protected JChannel select(ExecutorKey executorKey) {
        return null;

    }
}
