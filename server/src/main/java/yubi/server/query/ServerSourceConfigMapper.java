package yubi.server.query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import yubi.core.base.consts.Const;
import yubi.core.data.provider.DataProviderSource;
import yubi.core.entity.Source;
import yubi.security.util.AESUtil;

import java.util.HashMap;
import java.util.Map;

@Component
public class ServerSourceConfigMapper {

    private final ObjectMapper objectMapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public Map<String, Object> properties(Source source) {
        try {
            Map<String, Object> properties = new HashMap<>(16);
            if (StringUtils.isNotBlank(source.getConfig())) {
                properties = objectMapper.readValue(source.getConfig(), new TypeReference<>() {
                });
            }
            properties.replaceAll((key, value) -> value instanceof String text ? decrypt(text) : value);
            return properties;
        } catch (Exception ex) {
            throw new IllegalStateException("数据源配置无法解析", ex);
        }
    }

    public DataProviderSource providerSource(Source source) {
        DataProviderSource providerSource = new DataProviderSource();
        providerSource.setSourceId(source.getId());
        providerSource.setType(source.getType());
        providerSource.setName(source.getName());
        providerSource.setProperties(properties(source));
        return providerSource;
    }

    public String decrypt(String value) {
        if (StringUtils.isEmpty(value) || !value.startsWith(Const.ENCRYPT_FLAG)) {
            return value;
        }
        try {
            return AESUtil.decrypt(value.replaceFirst(Const.ENCRYPT_FLAG, ""));
        } catch (Exception ignored) {
            return value;
        }
    }
}
