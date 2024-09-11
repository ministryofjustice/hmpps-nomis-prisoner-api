package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.physicalattributes

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.trackEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributes
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPhysicalAttributesRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.getReleaseTimer

@Service
@Transactional
class PhysicalAttributesService(
  private val bookingRepository: OffenderBookingRepository,
  private val offenderRepository: OffenderRepository,
  private val offenderPhysicalAttributesRepository: OffenderPhysicalAttributesRepository,
  private val telemetryClient: TelemetryClient,
) {
  fun getPhysicalAttributes(offenderNo: String): PrisonerPhysicalAttributesResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("No offender found for $offenderNo")
    }

    return bookingRepository.findAllByOffenderNomsId(offenderNo)
      .filterNot { it.physicalAttributes.isEmpty() }
      .map {
        BookingPhysicalAttributesResponse(
          bookingId = it.bookingId,
          startDateTime = it.bookingBeginDate,
          endDateTime = it.getReleaseTimer(),
          latestBooking = it.bookingSequence == 1,
          physicalAttributes = it.physicalAttributes.map {
            PhysicalAttributesResponse(
              attributeSequence = it.id.sequence,
              heightCentimetres = it.getHeightInCentimetres(),
              weightKilograms = it.getWeightInKilograms(),
              createDateTime = it.createDatetime,
              createdBy = it.createUserId,
              modifiedDateTime = it.modifyDatetime,
              modifiedBy = it.modifyUserId,
              auditModuleName = it.auditModuleName,
            )
          },
        )
      }.let {
        PrisonerPhysicalAttributesResponse(offenderNo = offenderNo, bookings = it)
      }
  }

  fun upsertPhysicalAttributes(offenderNo: String, request: UpsertPhysicalAttributesRequest): UpsertPhysicalAttributesResponse {
    val booking = bookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("No latest booking found for $offenderNo")
    var created = true

    val physicalAttributes = booking.physicalAttributes.find { it.id.sequence == 1L }
      ?.also { created = false }
      ?: OffenderPhysicalAttributes(id = OffenderPhysicalAttributeId(booking, 1L))

    physicalAttributes.setWeightInKilograms(request.weight)
    physicalAttributes.setHeightInCentimetres(request.height)
    return offenderPhysicalAttributesRepository.save(physicalAttributes)
      .let {
        UpsertPhysicalAttributesResponse(
          bookingId = it.id.offenderBooking.bookingId,
          created = created,
        )
      }
      .also {
        val type = if (created) "created" else "updated"
        telemetryClient.trackEvent(
          "physical-attributes-$type",
          mutableMapOf("offenderNo" to offenderNo, "bookingId" to booking.bookingId.toString()),
        )
      }
  }
}
