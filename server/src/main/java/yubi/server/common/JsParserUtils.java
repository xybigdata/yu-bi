package yubi.server.common;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import yubi.core.base.exception.Exceptions;
import yubi.core.common.JavascriptUtils;
import yubi.server.base.params.DownloadCreateParam;

import javax.script.Invocable;
import javax.script.ScriptException;

public class JsParserUtils {

    private static Invocable parser;

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public static DownloadCreateParam parseExecuteParam(String type, String json) throws ScriptException, NoSuchMethodException {
        Invocable parser = getParser();
        if (parser == null) {
            Exceptions.msg("param parser load error");
        }
        Object result = parser.invokeFunction("getQueryData", type, json);
        return OBJECT_MAPPER.readValue(result.toString(), DownloadCreateParam.class);
    }

    private static synchronized Invocable getParser() {
        if (parser == null) {
            try {
                parser = JavascriptUtils.load("javascript/parser.js");
            } catch (Exception e) {
                Exceptions.e(e);
            }
        }
        return parser;
    }
}
