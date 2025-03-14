package com.simple.pulsejob.transport;

public abstract class Directory {

    private transient String appName;

    public String directoryString() {
        return appName;
    }
}
