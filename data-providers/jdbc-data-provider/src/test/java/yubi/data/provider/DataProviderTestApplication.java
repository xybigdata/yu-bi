package yubi.data.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"yubi.core.mappers","yubi.core.common"})
public class DataProviderTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataProviderTestApplication.class);
    }
}
