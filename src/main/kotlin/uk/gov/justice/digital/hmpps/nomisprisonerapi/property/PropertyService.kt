package uk.gov.justice.digital.hmpps.nomisprisonerapi.property

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPropertyContainer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPropertyContainerRepository

@Service
@Transactional
class PropertyService(
  private val offenderPropertyContainerRepository: OffenderPropertyContainerRepository,
  private val telemetryClient: TelemetryClient,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createProperty(propertyContainerCreateDto: PropertyContainerCreateDto) = CreatePropertyResponse(
    offenderPropertyContainerRepository.save(propertyContainerCreateDto.toModel()).propertyContainerId,
  )

  fun getProperty(propertyContainerId: Long) = offenderPropertyContainerRepository
    .findByIdOrNull(propertyContainerId)?.toDto()
    ?: throw NotFoundException("No property container with id $propertyContainerId")

  fun findIdsByFilter(pageRequest: Pageable, propertyFilter: PropertyFilter): Page<ContainerIdResponse> {
    log.info("Property request with page request $pageRequest")
    return offenderPropertyContainerRepository.findAll(pageRequest)
      .map { ContainerIdResponse(it.propertyContainerId) }
  }

  private fun PropertyContainerCreateDto.toModel() = OffenderPropertyContainer(
    offenderBooking = offenderBookingRepository.findByIdOrNull(bookingId)
      ?: throw BadDataException("Booking id $bookingId not found"),
    agencyInternalLocation = internalLocationId?.let {
      agencyInternalLocationRepository.findByIdOrNull(it)
        ?: throw BadDataException("Creating property for booking id $bookingId: No location with id $it found")
    },
    agencyLocation = agencyLocationRepository.findByIdOrNull(prisonId)
      ?: throw BadDataException("Creating property for booking id $bookingId: Prison $prisonId not found"),
    active = active,
    sealMark = sealMark,
    containerCode = containerCode,
    proposedDisposalDate = proposedDisposalDate,
    expiryDate = expiryDate,
  )

  private fun OffenderPropertyContainer.toDto() = PropertyContainerGetDto(
    containerId = propertyContainerId,
    offenderNo = offenderBooking.offender.nomsId,
    bookingId = offenderBooking.bookingId,
    internalLocationId = agencyInternalLocation?.locationId,
    prisonId = agencyLocation.id,
    active = active,
    sealMark = sealMark,
    containerCode = containerCode,
    expiryDate = expiryDate,
    proposedDisposalDate = proposedDisposalDate,
    createdDateTime = createDatetime,
    createdBy = createUsername,
  )
}
