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
package com.simple.pulsejob.transport.metadata;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import com.simple.pulsejob.common.util.Maps;
import com.simple.pulsejob.common.util.SystemPropertyUtil;
import lombok.Data;

/**
 * Request data wrapper.
 *
 * 请求消息包装.
 *
 * jupiter
 * org.jupiter.rpc.model.metadata
 *
 * @author jiachun.fjc
 */
@Data
public class MessageWrapper implements Serializable {

    @Serial
    private static final long serialVersionUID = 1009813828866652852L;
    private String executorName; //执行器名称
    private String jobBeanDefinitionName; //任务名称
    private Object[] args;                  // 目标方法参数

    public MessageWrapper(String executorName) {
        this.executorName = executorName;
    }
    public MessageWrapper(String jobBeanDefinitionName, Object[] args) {
        this.jobBeanDefinitionName = jobBeanDefinitionName;
        this.args = args;
    }

}
