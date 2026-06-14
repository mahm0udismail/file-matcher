package com.evision.file_matcher.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "filematcher")
public record FileMatcherProperties(

        @NotBlank(message = "filematcher.reference-file must not be blank")
        String referenceFile,

        @NotBlank(message = "filematcher.pool-directory must not be blank")
        String poolDirectory
) {}