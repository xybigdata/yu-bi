package yubi.server.service;

import org.springframework.stereotype.Component;
import yubi.core.common.TaskExecutor;

@Component
public class AsyncDownloadTaskScheduler implements DownloadTaskScheduler {

    @Override
    public void submit(Runnable task) {
        TaskExecutor.submit(task);
    }
}
