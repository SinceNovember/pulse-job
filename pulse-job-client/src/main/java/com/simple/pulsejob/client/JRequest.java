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
package com.simple.pulsejob.client;

import java.util.Collections;
import java.util.Map;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.client.model.metadata.MessageWrapper;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import lombok.Data;

/**
 * Consumer's request data.
 *
 * 请求信息载体.
 *
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
@Data
public class JRequest {

    private final JRequestPayload payload;   // 请求bytes/stream

    private MessageWrapper message;          // 请求对象

    public long invokeId() {
        return payload.invokeId();
    }

    public byte serializerCode() {
        return payload.serializerCode();
    }
}
