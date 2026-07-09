package uk.gov.justice.digital.hmpps.nomisprisonerapi.property

import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPropertyContainer
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPropertyContainerRepository

@Service
@Transactional
class PropertyService(
  private val offenderPropertyContainerRepository: OffenderPropertyContainerRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val agencyLocationRepository: AgencyLocationRepository,
  private val offenderBookingRepository: OffenderBookingRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun createProperty(propertyContainerCreateRequest: PropertyContainerCreateRequest) = CreatePropertyResponse(
    offenderPropertyContainerRepository.save(propertyContainerCreateRequest.toModel()).propertyContainerId,
  )

  fun updateProperty(id: Long, propertyContainerUpdateRequest: PropertyContainerUpdateRequest) {
    val container = offenderPropertyContainerRepository.findById(id).get()
    container.apply {
      containerCode = propertyContainerUpdateRequest.containerCode
      sealMark = propertyContainerUpdateRequest.sealMark
      agencyInternalLocation = findLocation(propertyContainerUpdateRequest.internalLocationId)
      proposedDisposalDate = propertyContainerUpdateRequest.proposedDisposalDate
    }
  }

  fun getProperty(propertyContainerId: Long) = offenderPropertyContainerRepository
    .findByIdOrNull(propertyContainerId)?.toDto()
    ?: throw NotFoundException("No property container with id $propertyContainerId")

  fun findIdsByFilter(pageRequest: Pageable, propertyFilter: PropertyFilter): Page<ContainerIdResponse> = if (
    propertyFilter.prisonIds.isNullOrEmpty()
  ) {
    offenderPropertyContainerRepository.findIds(pageRequest)
  } else {
    offenderPropertyContainerRepository.findIdsByAgencyLocation_IdIn(pageRequest, propertyFilter.prisonIds)
  }.map { ContainerIdResponse(it.propertyContainerId) }

  private fun PropertyContainerCreateRequest.toModel() = OffenderPropertyContainer(
    offenderBooking = findBooking(),
    agencyInternalLocation = findLocation(this.internalLocationId),
    agencyLocation = findAgency(),
    active = active,
    sealMark = sealMark,
    containerCode = containerCode,
    proposedDisposalDate = proposedDisposalDate,
    expiryDate = expiryDate,
  )

  private fun OffenderPropertyContainer.toDto() = PropertyContainerGetResponse(
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
    updatedDateTime = modifyDatetime,
    updatedBy = modifyUserId,
  )

  private fun PropertyContainerCreateRequest.findBooking(): OffenderBooking = offenderBookingRepository
    .findByIdOrNull(bookingId)
    ?: throw BadDataException("Booking id $bookingId not found")

  private fun findLocation(internalLocationId: Long?): AgencyInternalLocation? = internalLocationId?.let {
    agencyInternalLocationRepository.findByIdOrNull(it)
      ?: throw BadDataException("No location with id $it found")
  }

  private fun PropertyContainerCreateRequest.findAgency(): AgencyLocation = agencyLocationRepository
    .findByIdOrNull(prisonId)
    ?: throw BadDataException("Creating property for booking id $bookingId: Prison $prisonId not found")
}
