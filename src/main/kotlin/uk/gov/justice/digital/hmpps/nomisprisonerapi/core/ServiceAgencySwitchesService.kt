package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ExternalService
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ExternalServiceRepository

@Service
@Transactional
class ServiceAgencySwitchesService(
  private val externalServiceRepository: ExternalServiceRepository,
) {

  fun getServicePrisons(serviceCode: String): List<PrisonDetails> =
    findExternalServiceOrThrow(serviceCode)
      .serviceAgencySwitches
      .map { PrisonDetails(it.id.agencyLocation.id, it.id.agencyLocation.description) }

  private fun findExternalServiceOrThrow(serviceCode: String) =
    externalServiceRepository.findByIdOrNull(serviceCode)
      ?: throw NotFoundException("Service code $serviceCode does not exist")

  fun findAll(): List<ExternalService> = externalServiceRepository.findAll().toList()
}
