package com.openforum.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.openforum")
public class OpenForumApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenForumApplication.class, args);
    }

}
