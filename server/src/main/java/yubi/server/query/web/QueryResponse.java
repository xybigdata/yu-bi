package yubi.server.query.web;

import java.util.List;

public record QueryResponse(String id,
                            String name,
                            String vizType,
                            String vizId,
                            List<Column> columns,
                            List<List<Object>> rows,
                            PageInfo pageInfo,
                            String script) {

    public record Column(List<String> name, String type, String fmt, List<ForeignKey> foreignKeys) {
    }

    public record ForeignKey(String database, String table, String column) {
    }

    public record PageInfo(long pageNo, long pageSize, long total, boolean countTotal) {
    }
}
