package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ExternalService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitch
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ExternalServiceRepository

@DslMarker
annotation class ExternalServiceDslMarker

@NomisDataDslMarker
interface ExternalServiceDsl {

  @ServiceAgencySwitchDslMarker
  fun serviceAgencySwitch(
    prisonId: String = "BXI",
  ): ServiceAgencySwitch
}

@Component
class ExternalServiceBuilderRepository(private val externalServiceRepository: ExternalServiceRepository) {
  fun save(externalService: ExternalService): ExternalService =
    externalServiceRepository.findByIdOrNull(externalService.serviceName)
      ?: externalServiceRepository.save(externalService)
}

@Component
class ExternalServiceBuilderFactory(
  private val repository: ExternalServiceBuilderRepository,
  private val serviceAgencySwitchBuilderFactory: ServiceAgencySwitchBuilderFactory,
) {
  fun builder() = ExternalServiceBuilder(repository, serviceAgencySwitchBuilderFactory)
}

class ExternalServiceBuilder(
  private val repository: ExternalServiceBuilderRepository,
  private val serviceAgencySwitchBuilderFactory: ServiceAgencySwitchBuilderFactory,
) : ExternalServiceDsl {
  private lateinit var externalService: ExternalService

  fun build(
    serviceName: String,
    description: String,
  ): ExternalService = ExternalService(
    serviceName,
    description,
  )
    .let { repository.save(it) }
    .also { externalService = it }

  override fun serviceAgencySwitch(
    prisonId: String,
  ): ServiceAgencySwitch =
    serviceAgencySwitchBuilderFactory.builder().build(
      externalService,
      prisonId,
    )
      .also { externalService.serviceAgencySwitches += it }
}
