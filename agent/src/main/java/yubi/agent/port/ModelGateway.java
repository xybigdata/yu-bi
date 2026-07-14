package yubi.agent.port;

import yubi.agent.domain.ModelProtocol.ModelDecision;
import yubi.agent.domain.ModelProtocol.ModelTurn;

public interface ModelGateway {
    ModelDecision next(ModelTurn turn);
}
