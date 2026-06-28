package yubi.core.data.provider.processor;

import yubi.core.base.processor.ExtendProcessor;
import yubi.core.base.processor.ProcessorResponse;
import yubi.core.data.provider.DataProviderSource;
import yubi.core.data.provider.Dataframe;
import yubi.core.data.provider.ExecuteParam;
import yubi.core.data.provider.QueryScript;

public interface DataProviderPostProcessor extends ExtendProcessor {
    ProcessorResponse postRun(Dataframe frame, DataProviderSource config, QueryScript script, ExecuteParam executeParam);
}
