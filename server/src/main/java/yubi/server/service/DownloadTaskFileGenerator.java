package yubi.server.service;

import java.io.OutputStream;

@FunctionalInterface
public interface DownloadTaskFileGenerator {

    void generate(OutputStream target) throws Exception;
}
