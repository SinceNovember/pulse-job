/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.simple.pulsejob.transport.netty.handler.acceptor;

import com.simple.pulsejob.transport.exception.IoSignals;
import com.simple.pulsejob.transport.netty.channel.NettyChannel;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * jupiter
 * org.jupiter.transport.netty.handler.acceptor
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public class AcceptorIdleStateTrigger extends ChannelInboundHandlerAdapter {

    private final AcceptorProcessor processor;

    public AcceptorIdleStateTrigger(AcceptorProcessor processor) {
        this.processor = processor;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                if (processor != null) {
                    processor.handleInactive(NettyChannel.attachChannel(ctx.channel()));
                }
                throw IoSignals.READER_IDLE;
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
