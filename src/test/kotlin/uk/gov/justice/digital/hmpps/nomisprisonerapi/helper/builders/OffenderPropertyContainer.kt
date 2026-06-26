package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPropertyContainer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PropertyContainerCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPropertyContainerRepository
import java.time.LocalDate

@DslMarker
annotation class OffenderPropertyContainerDslMarker

@NomisDataDslMarker
interface OffenderPropertyContainerDsl

@Component
class OffenderPropertyContainerBuilderRepository(
  private val offenderPropertyContainerRepository: OffenderPropertyContainerRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
) {
  fun save(offenderPropertyContainer: OffenderPropertyContainer) = offenderPropertyContainerRepository
    .saveAndFlush(offenderPropertyContainer)

  fun lookupAgency(prisonId: String): AgencyLocation = agencyLocationRepository.findByIdOrNull(prisonId)
    ?: throw BadDataException("Prison $prisonId not found")

  fun lookupBox(id: Long?): AgencyInternalLocation? = id?.let {
    agencyInternalLocationRepository.findByIdOrNull(id) ?: throw BadDataException("Box $id not found")
  }
}

@Component
class OffenderPropertyContainerBuilderFactory(val repository: OffenderPropertyContainerBuilderRepository) {
  fun builder() = OffenderPropertyContainerBuilder(repository)
}

class OffenderPropertyContainerBuilder(
  private val repository: OffenderPropertyContainerBuilderRepository,
) : OffenderPropertyContainerDsl {
  fun build(
    booking: OffenderBooking,
    prisonId: String,
    internalLocationId: Long? = null,
    active: Boolean,
    sealMark: String,
    containerCode: PropertyContainerCode,
    expiryDate: LocalDate? = null,
    proposedDisposalDate: LocalDate? = null,
  ): OffenderPropertyContainer = OffenderPropertyContainer(
    offenderBooking = booking,
    agencyInternalLocation = repository.lookupBox(internalLocationId),
    agencyLocation = repository.lookupAgency(prisonId),
    active = active,
    sealMark = sealMark,
    containerCode = containerCode,
    proposedDisposalDate = proposedDisposalDate,
    expiryDate = expiryDate,
  )
    .let { repository.save(it) }
}
