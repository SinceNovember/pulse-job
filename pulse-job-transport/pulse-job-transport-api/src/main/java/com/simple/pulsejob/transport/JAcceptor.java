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
package com.simple.pulsejob.transport;

import java.net.SocketAddress;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;

/**
 * Server acceptor.
 *
 * 注意 JAcceptor 单例即可, 不要创建多个实例.
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public interface JAcceptor extends Transporter {

    /**
     * Local address.
     */
    SocketAddress localAddress();

    /**
     * Returns bound port.
     */
    int boundPort();

    /**
     * Acceptor options [parent, child].
     */
    JConfigGroup configGroup();

    /**
     * Returns the rpc processor.
     */
    AcceptorProcessor processor();

    /**
     * Binds the rpc processor.
     */
    void withProcessor(AcceptorProcessor processor);

    /**
     * Start the server and wait until the server socket is closed.
     */
    void start() throws InterruptedException;

    /**
     * Start the server.
     */
    void start(boolean sync) throws InterruptedException;

    /**
     * Shutdown the server gracefully.
     */
    void shutdownGracefully();
}
