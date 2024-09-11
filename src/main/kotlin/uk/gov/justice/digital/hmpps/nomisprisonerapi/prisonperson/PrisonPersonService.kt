package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.trackEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributes
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderPhysicalAttributesRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.BookingPhysicalAttributesResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.BookingProfileDetailsResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.PhysicalAttributesResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.PrisonPersonReconciliationResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.PrisonerPhysicalAttributesResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.PrisonerProfileDetailsResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.ProfileDetailsResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.UpsertPhysicalAttributesRequest
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.UpsertPhysicalAttributesResponse
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Service
@Transactional
class PrisonPersonService(
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
          endDateTime = it.getReleaseTime(),
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

  fun getProfileDetails(offenderNo: String): PrisonerProfileDetailsResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("No offender found for $offenderNo")
    }

    return bookingRepository.findAllByOffenderNomsId(offenderNo)
      .filterNot { it.profiles.isEmpty() }
      .map { booking ->
        BookingProfileDetailsResponse(
          bookingId = booking.bookingId,
          startDateTime = booking.bookingBeginDate,
          endDateTime = booking.getReleaseTime(),
          latestBooking = booking.bookingSequence == 1,
          profileDetails = booking.profiles.first().profileDetails
            .filter { pd -> pd.profileCodeId != null }
            .map { pd ->
              ProfileDetailsResponse(
                type = pd.id.profileType.type,
                code = pd.profileCodeId!!,
                createDateTime = pd.createDatetime,
                createdBy = pd.createUserId,
                modifiedDateTime = pd.modifyDatetime,
                modifiedBy = pd.modifyUserId,
                auditModuleName = pd.auditModuleName,
              )
            },
        ).takeIf { bookingResponse -> bookingResponse.profileDetails.isNotEmpty() }
      }
      .filterNotNull()
      .let { PrisonerProfileDetailsResponse(offenderNo = offenderNo, bookings = it) }
  }

  // NOMIS truncates the time from booking end date, so try and get the accurate time from the last release movement
  private fun OffenderBooking.getReleaseTime(): LocalDateTime? =
    takeIf { !active }
      ?.let {
        externalMovements
          .filter { it.movementType?.code == "REL" }
          .maxByOrNull { it.movementTime }
          ?.movementTime
          ?: bookingEndDate
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

  fun getReconciliation(offenderNo: String): PrisonPersonReconciliationResponse {
    if (!offenderRepository.existsByNomsId(offenderNo)) {
      throw NotFoundException("No offender found for $offenderNo")
    }

    val latestBooking = bookingRepository.findLatestByOffenderNomsId(offenderNo)
      ?: throw NotFoundException("No bookings found for $offenderNo")

    return latestBooking.physicalAttributes.minByOrNull { it.id.sequence }
      ?.let {
        PrisonPersonReconciliationResponse(
          offenderNo = offenderNo,
          height = it.getHeightInCentimetres(),
          weight = it.getWeightInKilograms(),
        )
      }
      ?: PrisonPersonReconciliationResponse(offenderNo = offenderNo)
  }

  // Note that the OffenderPhysicalAttributes extension functions below haven't been added to the class as getters and setters
  // because they only make sense in the context of this service. For example if JPA started using these methods then
  // OffenderPhysicalAttributes wouldn't represent the values found in NOMIS.
  private fun OffenderPhysicalAttributes.getHeightInCentimetres() =
    // Take height in cm if it exists because the data is more accurate (being a smaller unit than inches)
    if (heightCentimetres != null) {
      heightCentimetres
    } else {
      heightFeet?.let { ((it * 12) + (heightInches ?: 0)) * 2.54 }?.roundToInt()
    }

  private fun OffenderPhysicalAttributes.setHeightInCentimetres(value: Int?) {
    heightCentimetres = value
    val inches = heightCentimetres?.div(2.54)
    heightFeet = inches?.div(12)?.toInt()
    heightInches = inches?.rem(12)?.roundToInt()
  }

  private fun OffenderPhysicalAttributes.getWeightInKilograms() =
    // Take weight in lb and convert if it exists because the data is more accurate (being a smaller unit than kg). See the unit tests for an example explaining why.
    if (weightPounds != null) {
      weightPounds!!.let { (it * 0.453592) }.roundToInt()
    } else {
      weightKilograms
    }

  private fun OffenderPhysicalAttributes.setWeightInKilograms(value: Int?) {
    weightKilograms = value
    weightPounds = weightKilograms?.let { (it / 0.453592) }?.roundToInt()
  }
}
