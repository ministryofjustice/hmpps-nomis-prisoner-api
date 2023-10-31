package uk.gov.justice.digital.hmpps.nomisprisonerapi.health

import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.core.ServiceAgencySwitchesService

@Component
class FeatureSwitchInfoContributor(
  private val serviceAgencySwitchesService: ServiceAgencySwitchesService,
) : InfoContributor {

  @Transactional(readOnly = true)
  override fun contribute(builder: Info.Builder) {
    val switches = serviceAgencySwitchesService.findAll().map {
      mapOf(it.serviceName to it.serviceAgencySwitches.map { agency -> agency.id.agencyLocation.id })
    }
    builder.withDetail("agencySwitches", switches)
  }
}
