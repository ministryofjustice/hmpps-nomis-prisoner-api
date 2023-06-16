package uk.gov.justice.digital.hmpps.nomisprisonerapi.appointments

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.specification.AppointmentSpecification
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
            "eventId" to it.eventId.toString(),
            "bookingId" to it.offenderBooking.bookingId.toString(),
            "location" to it.internalLocation?.locationId.toString(),
          ),
          null,
        )
      }
      .let { CreateAppointmentResponse(offenderIndividualScheduleRepository.save(it).eventId) }

  fun updateAppointment(eventId: Long, dto: UpdateAppointmentRequest) {
    offenderIndividualScheduleRepository.findByIdOrNull(eventId)
      ?.apply {
        val location = agencyInternalLocationRepository.findByIdOrNull(dto.internalLocationId)
          ?: throw BadDataException("Room with id=${dto.internalLocationId} does not exist")

        val subType = eventSubTypeRepository.findByIdOrNull(EventSubType.pk(dto.eventSubType))
          ?: throw BadDataException("EventSubType with code=${dto.eventSubType} does not exist")

        if (dto.endTime < dto.startTime) {
          throw BadDataException("End time must be after start time")
        }

        internalLocation = location
        startTime = LocalDateTime.of(dto.eventDate, dto.startTime)
        endTime = LocalDateTime.of(dto.eventDate, dto.endTime)
        eventDate = dto.eventDate
        eventSubType = subType
        comment = dto.comment

        telemetryClient.trackEvent(
          "appointment-updated",
          mapOf(
            "eventId" to eventId.toString(),
            "bookingId" to offenderBooking.bookingId.toString(),
            "location" to internalLocation?.locationId.toString(),
          ),
          null,
        )
      }
      ?: throw NotFoundException("Appointment with event id $eventId not found")
  }

  fun cancelAppointment(eventId: Long) {
    offenderIndividualScheduleRepository.findByIdOrNull(eventId)
      ?.apply {
        eventStatus = eventStatusRepository.findById(EventStatus.CANCELLED).orElseThrow()

        telemetryClient.trackEvent(
          "appointment-cancelled",
          mapOf(
            "eventId" to eventId.toString(),
            "bookingId" to offenderBooking.bookingId.toString(),
            "location" to internalLocation?.locationId.toString(),
          ),
          null,
        )
      }
      ?: throw NotFoundException("Appointment with event id $eventId not found")
  }

  fun deleteAppointment(eventId: Long) {
    offenderIndividualScheduleRepository.findByIdOrNull(eventId)
      ?.also {
        offenderIndividualScheduleRepository.delete(it)
        telemetryClient.trackEvent(
          "appointment-deleted",
          mapOf(
            "eventId" to it.eventId.toString(),
            "bookingId" to it.offenderBooking.bookingId.toString(),
            "location" to it.internalLocation?.locationId.toString(),
          ),
          null,
        )
      }
      ?: throw NotFoundException("Appointment with event id $eventId not found")
  }

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
      comment = dto.comment,
    )
  }

  fun getAppointment(bookingId: Long, locationId: Long, date: LocalDateTime): AppointmentResponse =
    offenderIndividualScheduleRepository.findOneByBookingLocationDateAndStartTime(
      bookingId = bookingId,
      locationId = locationId,
      date = date.toLocalDate(),
      hour = date.hour,
      minute = date.minute,
    )?.let {
      return mapModel(it)
    }
      ?: throw NotFoundException("Appointment not found")

  fun getAppointment(eventId: Long): AppointmentResponse =
    offenderIndividualScheduleRepository.findByIdOrNull(eventId)?.let {
      return mapModel(it)
    }
      ?: throw NotFoundException("Appointment not found")

  fun findIdsByFilter(pageRequest: Pageable, appointmentFilter: AppointmentFilter): Page<AppointmentIdResponse> =
    offenderIndividualScheduleRepository.findAll(AppointmentSpecification(appointmentFilter), pageRequest)
      .map { AppointmentIdResponse(eventId = it.eventId) }
}

private fun mapModel(entity: OffenderIndividualSchedule): AppointmentResponse =
  AppointmentResponse(
    bookingId = entity.offenderBooking.bookingId,
    offenderNo = entity.offenderBooking.offender.nomsId,
    startDateTime = entity.startTime?.let { LocalDateTime.of(entity.eventDate, it.toLocalTime()) },
    endDateTime = entity.endTime?.let { LocalDateTime.of(entity.eventDate, it.toLocalTime()) },
    status = entity.eventStatus.code,
    subtype = entity.eventSubType.code,
    internalLocation = entity.internalLocation?.locationId,
    prisonId = entity.prison?.id ?: entity.toPrison?.id,
    comment = entity.comment,
    createdDate = entity.createdDate!!,
    createdBy = entity.createdBy!!,
    modifiedDate = entity.modifiedDate,
    modifiedBy = entity.modifiedBy,
  )
