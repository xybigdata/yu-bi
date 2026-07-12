package yubi.query.port;

import yubi.query.domain.QueryModels.EngineResult;
import yubi.query.domain.QueryModels.Plan;

public interface QueryEnginePort {
    EngineResult execute(Plan plan) throws Exception;
}
