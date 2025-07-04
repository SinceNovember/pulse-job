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
package com.simple.pulsejob.transport.processor;

import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;

/**
 * Consumer's processor.
 *
 * jupiter
 * org.jupiter.transport.processor
 *
 * @author jiachun.fjc
 */
public interface AcceptorProcessor {

    void handleRequest(JChannel channel, JRequestPayload request);

    void handleResponse(JChannel channel, JResponsePayload response);

    void handleActive(JChannel channel);

    void handleInactive(JChannel channel);

    void shutdown();
}
