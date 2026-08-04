package yubi.server.artifact;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;

final class FileSystemArtifactBlobStore implements ArtifactBlobStore {

    private final Path root;
    private final Path blobDirectory;
    private final Path temporaryDirectory;

    FileSystemArtifactBlobStore(Path root) {
        this.root = Objects.requireNonNull(root, "产物存储目录不能为空").toAbsolutePath().normalize();
        this.blobDirectory = this.root.resolve("blobs");
        this.temporaryDirectory = this.root.resolve("temporary");
        createDirectories(blobDirectory);
        createDirectories(temporaryDirectory);
    }

    @Override
    public ArtifactBlobWriter begin(String taskId) throws IOException {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("产物任务 ID 不能为空");
        }
        String blobKey = UUID.randomUUID() + ".blob";
        Path temporary = temporaryDirectory.resolve(blobKey + ".part");
        Path target = blobDirectory.resolve(blobKey);
        OutputStream output = Files.newOutputStream(temporary,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return new Writer(blobKey, temporary, target, output);
    }

    @Override
    public StoredBlob open(String blobKey) {
        Path file = blobPath(blobKey);
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("产物文件不存在");
        }
        try {
            return new StoredBlob(Files.size(file), Files.newInputStream(file, StandardOpenOption.READ));
        } catch (IOException exception) {
            throw new UncheckedIOException("读取产物文件失败", exception);
        }
    }

    @Override
    public void delete(String blobKey) {
        try {
            Files.deleteIfExists(blobPath(blobKey));
        } catch (IOException exception) {
            throw new UncheckedIOException("删除产物文件失败", exception);
        }
    }

    private Path blobPath(String blobKey) {
        if (blobKey == null || blobKey.isBlank()) {
            throw new IllegalArgumentException("blob key 不能为空");
        }
        try {
            Path key = Path.of(blobKey);
            if (key.isAbsolute() || key.getNameCount() != 1 || !key.getFileName().toString().equals(blobKey)) {
                throw new IllegalArgumentException("blob key 格式无效");
            }
            Path candidate = blobDirectory.resolve(key).normalize();
            if (!candidate.startsWith(blobDirectory)) {
                throw new IllegalArgumentException("blob key 不得越过产物目录");
            }
            return candidate;
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("blob key 格式无效", exception);
        }
    }

    private static void createDirectories(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new UncheckedIOException("创建产物存储目录失败", exception);
        }
    }

    private final class Writer implements ArtifactBlobWriter {

        private final String blobKey;
        private final Path temporary;
        private final Path target;
        private final OutputStream output;
        private State state = State.OPEN;

        private Writer(String blobKey, Path temporary, Path target, OutputStream output) {
            this.blobKey = blobKey;
            this.temporary = temporary;
            this.target = target;
            this.output = output;
        }

        @Override
        public OutputStream output() {
            if (state != State.OPEN) {
                throw new IllegalStateException("产物写入已结束");
            }
            return output;
        }

        @Override
        public String commit() {
            if (state == State.COMMITTED) {
                return blobKey;
            }
            if (state != State.OPEN) {
                throw new IllegalStateException("已终止的产物写入不能提交");
            }
            try {
                output.close();
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                state = State.COMMITTED;
                return blobKey;
            } catch (AtomicMoveNotSupportedException exception) {
                throw new UncheckedIOException("产物目录不支持原子发布", exception);
            } catch (IOException exception) {
                throw new UncheckedIOException("发布产物文件失败", exception);
            }
        }

        @Override
        public void abort() {
            if (state == State.ABORTED) {
                return;
            }
            IOException failure = null;
            try {
                output.close();
            } catch (IOException exception) {
                failure = exception;
            }
            try {
                Files.deleteIfExists(temporary);
                if (state == State.COMMITTED) {
                    Files.deleteIfExists(target);
                }
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
            state = State.ABORTED;
            if (failure != null) {
                throw new UncheckedIOException("清理未完成的产物文件失败", failure);
            }
        }

        @Override
        public void close() {
            if (state == State.OPEN) {
                abort();
            }
        }
    }

    private enum State {
        OPEN,
        COMMITTED,
        ABORTED
    }
}
