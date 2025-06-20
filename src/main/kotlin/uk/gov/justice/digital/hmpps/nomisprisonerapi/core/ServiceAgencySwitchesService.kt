package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitch
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ServiceAgencySwitchId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ExternalServiceRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ServiceAgencySwitchesRepository

@Service
@Transactional
class ServiceAgencySwitchesService(
  private val externalServiceRepository: ExternalServiceRepository,
  private val serviceAgencySwitchesRepository: ServiceAgencySwitchesRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val bookingRepository: OffenderBookingRepository,
) {

  fun getServicePrisons(serviceCode: String): List<PrisonDetails> = findExternalServiceOrThrow(serviceCode)
    .serviceAgencySwitches
    .map { PrisonDetails(it.id.agencyLocation.id, it.id.agencyLocation.description) }

  fun getServiceAgencies(serviceCode: String): List<AgencyDetails> = findExternalServiceOrThrow(serviceCode)
    .serviceAgencySwitches
    .map { AgencyDetails(it.id.agencyLocation.id, it.id.agencyLocation.description) }

  private fun findExternalServiceOrThrow(serviceCode: String) = externalServiceRepository.findByIdOrNull(serviceCode)
    ?: throw NotFoundException("Service code $serviceCode does not exist")

  fun checkServiceAgency(serviceCode: String, prisonId: String): Boolean = serviceAgencySwitchesRepository.checkServiceAgencyAndAll(serviceCode, prisonId)

  fun checkServiceAgencyForPrisoner(serviceCode: String, prisonNumber: String): Boolean {
    val prisonId = bookingRepository.findLatestByOffenderNomsId(prisonNumber)?.location?.id
      ?: throw NotFoundException("No prisoner with offender $prisonNumber found")
    return checkServiceAgency(serviceCode, prisonId)
  }

  fun createServiceAgency(serviceCode: String, agencyId: String) {
    val service = externalServiceRepository.findByIdOrNull(serviceCode) ?: throw NotFoundException("Service $serviceCode does not exist")
    val agency = agencyLocationRepository.findByIdOrNull(agencyId) ?: throw NotFoundException("Agency $agencyId does not exist")
    serviceAgencySwitchesRepository.save(ServiceAgencySwitch(ServiceAgencySwitchId(service, agency)))
  }
}
