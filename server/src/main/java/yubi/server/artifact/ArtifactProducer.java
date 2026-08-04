package yubi.server.artifact;

@FunctionalInterface
public interface ArtifactProducer {

    void produce(ArtifactWorkContext context) throws Exception;
}
