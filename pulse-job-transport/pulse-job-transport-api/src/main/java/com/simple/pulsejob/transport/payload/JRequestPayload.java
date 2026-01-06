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
 * 请求的消息体bytes/stream载体, 避免在IO线程中序列化/反序列化, jupiter-transport这一层不关注消息体的对象结构.
 *
 * jupiter
 * org.jupiter.transport.payload
 *
 * @author jiachun.fjc
 */
public class JRequestPayload extends PayloadHolder {

    // instanceId 来自数据库 job_instance.instanceId，用于任务追踪和日志关联
    private long instanceId;

    // jupiter-transport层会在协议解析完成后打上一个时间戳, 用于后续监控对该请求的处理时间
    private transient long timestamp;

    public JRequestPayload(long instanceId) {
        this.instanceId = instanceId;
    }

    public long instanceId() {
        return instanceId;
    }

    public long timestamp() {
        return timestamp;
    }

    public void timestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
