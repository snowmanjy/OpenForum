package com.openforum.admin;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.openforum.admin", excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.openforum\\.admin\\.integration\\..*"))
public class SliceTestConfig {
}
