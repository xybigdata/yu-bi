package yubi.core.data.provider.processor;

import yubi.core.base.processor.ExtendProcessor;
import yubi.core.base.processor.ProcessorResponse;
import yubi.core.data.provider.DataProviderSource;
import yubi.core.data.provider.ExecuteParam;
import yubi.core.data.provider.QueryScript;

public interface DataProviderPreProcessor extends ExtendProcessor {
    ProcessorResponse preRun(DataProviderSource config, QueryScript script, ExecuteParam executeParam);
}
