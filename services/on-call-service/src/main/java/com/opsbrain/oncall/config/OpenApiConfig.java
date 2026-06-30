package com.opsbrain.oncall.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "On-Call Service API",
                version = "1.0.0",
                description = "API for managing on-call schedules and rotations in OpsBrain",
                contact = @Contact(
                        name = "OpsBrain Team"
                ),
                license = @License(
                        name = "Apache 2.0"
                )
        )
)
public class OpenApiConfig {
}
