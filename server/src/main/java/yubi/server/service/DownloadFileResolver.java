package yubi.server.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import yubi.core.base.consts.FileOwner;
import yubi.core.base.exception.NotAllowedException;
import yubi.core.common.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

@Component
public class DownloadFileResolver {

    private static final String FILE_UNAVAILABLE = "下载文件不可用";
    private static final Set<PosixFilePermission> OWNER_DIRECTORY_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
    );
    private static final Set<PosixFilePermission> OWNER_FILE_PERMISSIONS = Set.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE
    );

    private final DirectoryOpenHook directoryOpenHook;

    public DownloadFileResolver() {
        this((operation, taskDirectory) -> {
        });
    }

    DownloadFileResolver(DirectoryOpenHook directoryOpenHook) {
        this.directoryOpenHook = directoryOpenHook;
    }

    public Path createTaskDirectory(String taskId) {
        Path root = ensureRoot();
        Path directory = resolveTaskDirectory(root, singlePathSegment(taskId));
        try {
            Files.createDirectory(directory);
            setOwnerPermissions(directory, OWNER_DIRECTORY_PERMISSIONS);
            return directory;
        } catch (IOException | RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    public DownloadFileTarget createTarget(Path taskDirectory, String fileName) {
        Path directory = requireTaskDirectory(taskDirectory);
        Path target = resolveChild(directory, singlePathSegment(fileName));
        try {
            directoryOpenHook.afterTaskDirectoryOpened(Operation.CREATE, directory);
            SeekableByteChannel channel = Files.newByteChannel(
                    target,
                    Set.<OpenOption>of(
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE,
                            LinkOption.NOFOLLOW_LINKS
                    )
            );
            try {
                setOwnerPermissions(target, OWNER_FILE_PERMISSIONS);
                OutputStream output = Channels.newOutputStream(channel);
                return new DownloadFileTarget(output, storedPath(directory.getFileName().toString(), target.getFileName().toString()),
                        target.getFileName().toString());
            } catch (IOException | RuntimeException exception) {
                try {
                    channel.close();
                } catch (IOException closeFailure) {
                    exception.addSuppressed(closeFailure);
                }
                throw exception;
            }
        } catch (IOException | RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    public String copyIntoTask(Path taskDirectory, Path stagingDirectory, Path generatedFile) {
        Path source = requireStagingFile(stagingDirectory, generatedFile);
        try (SeekableByteChannel sourceChannel = Files.newByteChannel(
                source,
                Set.<OpenOption>of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)
        ); DownloadFileTarget target = createTarget(taskDirectory, source.getFileName().toString())) {
            Channels.newInputStream(sourceChannel).transferTo(target.outputStream());
            return target.storedPath();
        } catch (IOException | RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    public DownloadFileResource openForRead(String storedPath) {
        StoredFile storedFile = parseStoredPath(storedPath);
        Path directory = requireTaskDirectory(ensureRoot().resolve(storedFile.taskId()));
        Path candidate = resolveChild(directory, storedFile.fileName());
        try {
            directoryOpenHook.afterTaskDirectoryOpened(Operation.READ, directory);
            if (Files.isSymbolicLink(candidate)
                    || !Files.isRegularFile(candidate, LinkOption.NOFOLLOW_LINKS)) {
                throw new NotAllowedException(FILE_UNAVAILABLE);
            }
            SeekableByteChannel channel = Files.newByteChannel(
                    candidate,
                    Set.<OpenOption>of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)
            );
            return new DownloadFileResource(Channels.newInputStream(channel), storedFile.fileName().toString());
        } catch (IOException | RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    public Path downloadRoot() {
        return ensureRoot();
    }

    private Path ensureRoot() {
        try {
            Path root = Path.of(FileUtils.withBasePath(FileOwner.DOWNLOAD.getPath())).toAbsolutePath().normalize();
            Files.createDirectories(root);
            if (Files.isSymbolicLink(root) || !Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
                throw new NotAllowedException(FILE_UNAVAILABLE);
            }
            return root;
        } catch (IOException | RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    private Path requireTaskDirectory(Path taskDirectory) {
        Path root = ensureRoot();
        if (taskDirectory == null) {
            throw new NotAllowedException(FILE_UNAVAILABLE);
        }
        Path directory = taskDirectory.toAbsolutePath().normalize();
        if (!root.equals(directory.getParent())
                || Files.isSymbolicLink(directory)
                || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            throw new NotAllowedException(FILE_UNAVAILABLE);
        }
        singlePathSegment(directory.getFileName().toString());
        return directory;
    }

    private Path requireStagingFile(Path stagingDirectory, Path generatedFile) {
        if (stagingDirectory == null || generatedFile == null) {
            throw new NotAllowedException(FILE_UNAVAILABLE);
        }
        try {
            Path staging = stagingDirectory.toAbsolutePath().normalize();
            Path source = generatedFile.toAbsolutePath().normalize();
            if (!staging.equals(source.getParent())
                    || Files.isSymbolicLink(staging)
                    || !Files.isDirectory(staging, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(source)
                    || !Files.isRegularFile(source, LinkOption.NOFOLLOW_LINKS)) {
                throw new NotAllowedException(FILE_UNAVAILABLE);
            }
            singlePathSegment(source.getFileName().toString());
            return source;
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    private Path resolveTaskDirectory(Path root, Path taskId) {
        Path directory = root.resolve(taskId).normalize();
        if (!root.equals(directory.getParent())) {
            throw new NotAllowedException(FILE_UNAVAILABLE);
        }
        return directory;
    }

    private Path resolveChild(Path directory, Path child) {
        Path candidate = directory.resolve(child).normalize();
        if (!directory.equals(candidate.getParent())) {
            throw new NotAllowedException(FILE_UNAVAILABLE);
        }
        return candidate;
    }

    private StoredFile parseStoredPath(String value) {
        if (StringUtils.isBlank(value)) {
            throw new NotAllowedException(FILE_UNAVAILABLE);
        }
        try {
            Path supplied = Path.of(value);
            if (supplied.isAbsolute() || supplied.getNameCount() != 3
                    || !"download".equals(supplied.getName(0).toString()) || containsTraversalSegment(supplied)) {
                throw new NotAllowedException(FILE_UNAVAILABLE);
            }
            return new StoredFile(singlePathSegment(supplied.getName(1).toString()),
                    singlePathSegment(supplied.getName(2).toString()));
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    private String storedPath(String taskId, String fileName) {
        return FileUtils.concatPath(FileOwner.DOWNLOAD.getPath(), FileUtils.concatPath(taskId, fileName));
    }

    private Path singlePathSegment(String value) {
        if (StringUtils.isBlank(value)) {
            throw new NotAllowedException(FILE_UNAVAILABLE);
        }
        try {
            Path relative = Path.of(value);
            if (relative.isAbsolute() || relative.getNameCount() != 1 || ".".equals(value) || "..".equals(value)) {
                throw new NotAllowedException(FILE_UNAVAILABLE);
            }
            return relative;
        } catch (RuntimeException exception) {
            throw unavailable(exception);
        }
    }

    private boolean containsTraversalSegment(Path path) {
        for (Path segment : path) {
            if (".".equals(segment.toString()) || "..".equals(segment.toString())) {
                return true;
            }
        }
        return false;
    }

    private void setOwnerPermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException ignored) {
            // 发行支持的存储由应用控制；不在本轮声明跨平台权限语义。
        }
    }

    private NotAllowedException unavailable(Exception exception) {
        return exception instanceof NotAllowedException notAllowed ? notAllowed : new NotAllowedException(FILE_UNAVAILABLE);
    }

    enum Operation { READ, CREATE }

    @FunctionalInterface
    interface DirectoryOpenHook {
        void afterTaskDirectoryOpened(Operation operation, Path taskDirectory) throws IOException;
    }

    private record StoredFile(Path taskId, Path fileName) {
    }
}
