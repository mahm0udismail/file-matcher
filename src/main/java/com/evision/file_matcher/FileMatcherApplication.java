package com.evision.file_matcher;

import com.evision.file_matcher.config.FileMatcherProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(FileMatcherProperties.class)
public class FileMatcherApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileMatcherApplication.class, args);
	}

}
