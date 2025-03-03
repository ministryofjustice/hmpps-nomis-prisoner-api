package uk.gov.justice.digital.hmpps.nomisprisonerapi.config

import io.swagger.v3.core.util.PrimitiveType
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.boot.info.BuildProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration(buildProperties: BuildProperties) {
  private val version: String = buildProperties.version

  @Bean
  fun customOpenAPI(): OpenAPI = OpenAPI()
    .servers(
      listOf(
        Server().url("https://nomis-prisoner-api-dev.prison.service.justice.gov.uk").description("Development"),
        Server().url("https://nomis-prisoner-api-preprod.prison.service.justice.gov.uk").description("PreProd"),
        Server().url("https://nomis-prisoner-api.prison.service.justice.gov.uk").description("Prod"),
        Server().url("http://localhost:8080").description("Local"),
      ),
    )
    .info(
      Info().title("NOMIS Synchronisation API")
        .version(version)
        .description("Controls writing Prisoner information back to NOMIS for data synchronisation (not intended for general usage)")
        .contact(Contact().name("HMPPS Digital Studio").email("feedback@digital.justice.gov.uk")),
    )
    .components(
      Components().addSecuritySchemes(
        "bearer-jwt",
        SecurityScheme()
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT")
          .`in`(SecurityScheme.In.HEADER)
          .name("Authorization"),
      ),
    )
    .addSecurityItem(SecurityRequirement().addList("bearer-jwt", listOf("read", "write")))

  @Bean
  fun openAPICustomiser(): OpenApiCustomizer = OpenApiCustomizer { }.also {
    PrimitiveType.enablePartialTime() // Prevents generation of a LocalTime schema which causes conflicts with java.time.LocalTime
  }
}
