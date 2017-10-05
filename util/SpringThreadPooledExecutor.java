package com.americanexpress.smartserviceengine.common.util;

import java.util.concurrent.ExecutorService;

import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ExecutorServiceAdapter;

public class SpringThreadPooledExecutor {

    private TaskExecutor executor;
    protected ExecutorService executorService;

    public ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = new ExecutorServiceAdapter(executor);
        }
        return executorService;
    }

    public void setExecutor(TaskExecutor executor) {
        this.executor = executor;
    }

}
