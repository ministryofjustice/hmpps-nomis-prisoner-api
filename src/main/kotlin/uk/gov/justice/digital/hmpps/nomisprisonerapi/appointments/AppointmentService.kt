package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.BadDataException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIndividualSchedule
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderIndividualScheduleRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDateTime

@Service
@Transactional
class AppointmentService(
  private val offenderBookingRepository: OffenderBookingRepository,
  private val offenderIndividualScheduleRepository: OffenderIndividualScheduleRepository,
  private val agencyInternalLocationRepository: AgencyInternalLocationRepository,
  private val eventStatusRepository: ReferenceCodeRepository<EventStatus>,
  private val eventSubTypeRepository: ReferenceCodeRepository<EventSubType>,
  private val telemetryClient: TelemetryClient,
) {
  fun createAppointment(dto: CreateAppointmentRequest): CreateAppointmentResponse =
    mapModel(dto)
      .also {
        telemetryClient.trackEvent(
          "appointment-created",
          mapOf(
            "id" to it.eventId.toString(),
            "bookingId" to it.offenderBooking.bookingId.toString(),
            "location" to it.internalLocation?.locationId.toString(),
          ),
          null
        )
      }
      .let { CreateAppointmentResponse(offenderIndividualScheduleRepository.save(it).eventId) }

  private fun mapModel(dto: CreateAppointmentRequest): OffenderIndividualSchedule {

    val offenderBooking = offenderBookingRepository.findByIdOrNull(dto.bookingId)
      ?: throw BadDataException("Booking with id=${dto.bookingId} not found")

    val location = agencyInternalLocationRepository.findByIdOrNull(dto.internalLocationId)
      ?: throw BadDataException("Room with id=${dto.internalLocationId} does not exist")

    if (location.agencyId != offenderBooking.location?.id) {
      throw BadDataException("Room with id=${dto.internalLocationId} is in ${location.agencyId}, not in the offender's prison: ${offenderBooking.location?.id}")
    }

    if (dto.endTime < dto.startTime) {
      throw BadDataException("End time must be after start time")
    }

    val eventSubType = eventSubTypeRepository.findByIdOrNull(EventSubType.pk(dto.eventSubType))
      ?: throw BadDataException("EventSubType with code=${dto.eventSubType} does not exist")

    return OffenderIndividualSchedule(
      offenderBooking = offenderBooking,
      eventDate = dto.eventDate,
      startTime = LocalDateTime.of(dto.eventDate, dto.startTime),
      endTime = LocalDateTime.of(dto.eventDate, dto.endTime),
      prison = offenderBooking.location,
      eventStatus = eventStatusRepository.findById(EventStatus.SCHEDULED_APPROVED).orElseThrow(),
      internalLocation = location,
      eventSubType = eventSubType,
    )
  }

  fun getAppointment(bookingId: Long, locationId: Long, date: LocalDateTime): AppointmentResponse =
    offenderIndividualScheduleRepository.findOneByBookingLocationDateAndStartTime(
      bookingId = bookingId,
      locationId = locationId,
      date = date.toLocalDate(),
      hour = date.hour,
      minute = date.minute
    )?.let {
      return mapModel(it)
    }
      ?: throw NotFoundException("Appointment not found")
}

private fun mapModel(entity: OffenderIndividualSchedule): AppointmentResponse {
  return AppointmentResponse(
    bookingId = entity.offenderBooking.bookingId,
  )
}
