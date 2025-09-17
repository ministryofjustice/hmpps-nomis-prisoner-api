package uk.gov.justice.digital.hmpps.nomisprisonerapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.hmpps.kotlin.auth.dsl.ResourceServerConfigurationCustomizer

@Configuration
class ResourceServerConfiguration {
  @Bean
  fun resourceServerCustomizer() = ResourceServerConfigurationCustomizer {
    anyRequestRole { defaultRole = "NOMIS_PRISONER_API__SYNCHRONISATION__RW" }
  }
}
