package com.simple.pulsejob.admin.scheduler.future;

import java.util.concurrent.CompletionStage;

public interface InvokeFuture extends CompletionStage<Object> {

    Object getResult() throws Throwable;

}
