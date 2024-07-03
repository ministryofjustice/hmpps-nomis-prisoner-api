package uk.gov.justice.digital.hmpps.nomisprisonerapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.stereotype.Service
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import java.util.Optional

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@Service(value = "auditorAware")
class AuditorAwareImpl(private val authenticationHolder: HmppsAuthenticationHolder) : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> {
    return Optional.ofNullable(authenticationHolder.principal)
  }
}
