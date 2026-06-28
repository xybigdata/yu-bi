package yubi;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"yubi"})
public class YuBiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(YuBiServerApplication.class, args);
    }

}