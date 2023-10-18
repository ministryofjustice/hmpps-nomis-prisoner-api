package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ExternalService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitch
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitchId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository

@DslMarker
annotation class ServiceAgencySwitchDslMarker

@NomisDataDslMarker
interface ServiceAgencySwitchDsl

@Component
class ServiceAgencySwitchBuilderRepository(val agencyLocationRepository: AgencyLocationRepository) {
  fun lookupAgency(id: String) = agencyLocationRepository.findByIdOrNull(id)!!
}

@Component
class ServiceAgencySwitchBuilderFactory(val repository: ServiceAgencySwitchBuilderRepository) {
  fun builder() = ServiceAgencySwitchBuilder(repository)
}

@Component
class ServiceAgencySwitchBuilder(val repository: ServiceAgencySwitchBuilderRepository) : ServiceAgencySwitchDsl {
  fun build(
    externalService: ExternalService,
    prisonId: String,
  ) =
    ServiceAgencySwitch(
      ServiceAgencySwitchId(
        externalService = externalService,
        agencyLocation = repository.lookupAgency(prisonId),
      ),
    )
}
