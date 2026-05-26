package com.pipelineforge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
	@Bean
	public OpenAPI pipelineForgeOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("PipelineForge API")
						.description("Backend API for pipeline and deployment orchestration")
						.version("v1")
						.license(new License().name("Proprietary")))
				.addSecurityItem(new io.swagger.v3.oas.models.security.SecurityRequirement().addList("BearerAuth"))
				.components(new io.swagger.v3.oas.models.Components()
						.addSecuritySchemes("BearerAuth", new io.swagger.v3.oas.models.security.SecurityScheme()
								.name("BearerAuth")
								.type(io.swagger.v3.oas.models.security.SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")));
	}
}
