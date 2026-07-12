package yubi.agent.port;

import java.time.Instant;

public interface WriteClockPort {

    Instant now();
}
