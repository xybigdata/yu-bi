package yubi.data.provider;

import yubi.core.data.provider.Column;
import yubi.core.data.provider.Dataframe;
import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.IOException;
import java.util.List;

public interface HttpResponseParser {

    Dataframe parseResponse(String targetPropertyName, ClassicHttpResponse response, List<Column> columns) throws IOException;

}
