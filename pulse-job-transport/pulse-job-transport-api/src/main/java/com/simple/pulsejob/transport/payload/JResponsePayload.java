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
package com.simple.pulsejob.transport.payload;

/**
 * 响应的消息体bytes/stream载体, 避免在IO线程中序列化/反序列化, jupiter-transport这一层不关注消息体的对象结构.
 *
 * jupiter
 * org.jupiter.transport.payload
 *
 * @author jiachun.fjc
 */
public class JResponsePayload extends PayloadHolder {

    // 用于映射 <instanceId, request, response> 三元组
    // instanceId = job_instance.instanceId
    private final long instanceId;
    private byte status;

    public JResponsePayload(long instanceId) {
        this.instanceId = instanceId;
    }

    public long instanceId() {
        return instanceId;
    }

    public byte status() {
        return status;
    }

    public void status(byte status) {
        this.status = status;
    }
}
