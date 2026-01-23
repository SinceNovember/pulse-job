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
package com.simple.pulsejob.admin.scheduler.future;

import java.util.concurrent.CompletableFuture;

import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.transport.JResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 用于实现fail-safe集群容错方案的 {@link InvokeFuture}.
 *
 * 同步调用时发生异常时只打印日志.
 *
 * jupiter
 * org.jupiter.rpc.consumer.future
 *
 * @see
 *
 * @author jiachun.fjc
 */
@Slf4j
public class FailsafeInvokeFuture extends CompletableFuture<JResponse> implements InvokeFuture {


    private final InvokeFuture future;

    public static FailsafeInvokeFuture with(InvokeFuture future) {
        return new FailsafeInvokeFuture(future);
    }

    private FailsafeInvokeFuture(InvokeFuture future) {
        this.future = future;
    }


    @Override
    public JResponse getResult() throws Throwable {
        try {
            return future.getResult();
        } catch (Throwable t) {
            if (log.isWarnEnabled()) {
                log.warn("Ignored exception on [Fail-safe]: {}.", StackTraceUtil.stackTrace(t));
            }
        }
        return null;
    }
}
