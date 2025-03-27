package com.simple.pulsejob.client.invoker;

import com.simple.pulsejob.client.JobContext;

public interface Invoker {

    Object invoke(JobContext jobContext);
}
