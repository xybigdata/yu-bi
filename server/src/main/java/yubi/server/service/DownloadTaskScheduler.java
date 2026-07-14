package yubi.server.service;

@FunctionalInterface
public interface DownloadTaskScheduler {

    void submit(Runnable task);
}
