package yubi.agent.application;

import yubi.agent.domain.AgentModels.ArgumentSummary;
import yubi.agent.domain.StructuredValue;
import yubi.agent.domain.StructuredValue.ArrayValue;
import yubi.agent.domain.StructuredValue.ObjectValue;
import yubi.query.api.MetadataToolSchema;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;

final class ArgumentSummarizer {

    ArgumentSummary summarize(ObjectValue arguments,
                              MetadataToolSchema schema,
                              int maximumNodes,
                              int maximumDepth) {
        if (arguments == null) {
            return ArgumentSummary.empty();
        }
        List<String> allowed = schema == null
                ? List.of()
                : schema.inputSchema().properties().keySet().stream().toList();
        List<String> recognized = allowed.stream().filter(arguments.values()::containsKey).toList();
        int rejected = arguments.values().size() - recognized.size();
        Counts counts = count(arguments, maximumNodes, maximumDepth);
        return new ArgumentSummary(recognized, Math.max(rejected, 0), counts.scalars,
                counts.collections, counts.depth);
    }

    private Counts count(StructuredValue root, int maximumNodes, int maximumDepth) {
        ArrayDeque<Node> pending = new ArrayDeque<>();
        pending.add(new Node(root, 1));
        int scalars = 0;
        int collections = 0;
        int depth = 0;
        int visited = 0;
        while (!pending.isEmpty()) {
            Node node = pending.removeFirst();
            depth = Math.max(depth, node.depth());
            if (node.depth() > maximumDepth || ++visited > maximumNodes) {
                return new Counts(maximumNodes + 1, collections, depth);
            }
            Collection<StructuredValue> children;
            if (node.value() instanceof ObjectValue object) {
                collections++;
                children = object.values().values();
            } else if (node.value() instanceof ArrayValue array) {
                collections++;
                children = array.values();
            } else {
                scalars++;
                continue;
            }
            if (visited + pending.size() + children.size() > maximumNodes) {
                return new Counts(maximumNodes + 1, collections, Math.max(depth, node.depth() + 1));
            }
            for (StructuredValue child : children) {
                pending.addLast(new Node(child, node.depth() + 1));
            }
        }
        return new Counts(scalars, collections, depth);
    }

    private record Counts(int scalars, int collections, int depth) {
    }

    private record Node(StructuredValue value, int depth) {
    }
}
