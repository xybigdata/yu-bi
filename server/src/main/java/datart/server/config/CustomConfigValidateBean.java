package datart.server.config;

import lombok.Data;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * 校验配置文件中的key规则
 */
@Data
@Validated
public class CustomConfigValidateBean {

    public static final String DATASOURCE_IP = "datasource.ip";

    public static final String DATASOURCE_PORT = "datasource.port";

    public static final String DATASOURCE_DATABASE = "datasource.database";

    public static final String DATASOURCE_USERNAME = "datasource.username";

    public static final String DATASOURCE_PASSWORD = "datasource.password";

    @NotBlank
    private String datasourceIp;

    @NotBlank
    private String datasourcePort;

    @NotBlank
    private String datasourceDatabase;

    @NotBlank
    private String datasourceUsername;

    @NotBlank
    private String datasourcePassword;

}
