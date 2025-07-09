package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Offender temporary absences by booking, including applications and scheduled absences")
data class OffenderTemporaryAbsencesResponse(
  @Schema(description = "List of bookings with their temporary absences and external movements")
  val bookings: List<BookingTemporaryAbsencesResponse>,
)

@Schema(description = "Booking temporary absences")
data class BookingTemporaryAbsencesResponse(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Temporary absence applications")
  val temporaryAbsenceApplications: List<TemporaryAbsenceApplicationResponse>,

  @Schema(description = "Unscheduled temporary absences OUT - those without an application or a schedule")
  val unscheduledTemporaryAbsences: List<TemporaryAbsenceResponse>,

  @Schema(description = "Unscheduled temporary absences IN - those without an application or a schedule")
  val unscheduledTemporaryAbsenceReturns: List<TemporaryAbsenceReturnResponse>,
)

@Schema(description = "Temporary absence application response")
data class TemporaryAbsenceApplicationResponse(
  @Schema(description = "Movement application ID")
  val movementApplicationId: Long,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Application date")
  val applicationDate: LocalDateTime,

  @Schema(description = "From date")
  val fromDate: LocalDate,

  @Schema(description = "Release time")
  val releaseTime: LocalDateTime,

  @Schema(description = "To date")
  val toDate: LocalDate,

  @Schema(description = "Return time")
  val returnTime: LocalDateTime,

  @Schema(description = "Application status")
  val applicationStatus: String,

  @Schema(description = "Escort code")
  val escortCode: String?,

  @Schema(description = "Transport type")
  val transportType: String?,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Prison ID")
  val prisonId: String?,

  @Schema(description = "To agency ID")
  val toAgencyId: String?,

  @Schema(description = "To address ID")
  val toAddressId: Long?,

  @Schema(description = "To address owner class")
  val toAddressOwnerClass: String?,

  @Schema(description = "Contact person name")
  val contactPersonName: String?,

  @Schema(description = "Application type")
  val applicationType: String,

  @Schema(description = "Temporary absence type")
  val temporaryAbsenceType: String?,

  @Schema(description = "Temporary absence sub type")
  val temporaryAbsenceSubType: String?,

  @Schema(description = "Scheduled temporary absence")
  val scheduledTemporaryAbsence: ScheduledTemporaryAbsenceResponse? = null,

  @Schema(description = "Scheduled temporary absence return")
  val scheduledTemporaryAbsenceReturn: ScheduledTemporaryAbsenceReturnResponse? = null,

  @Schema(description = "Temporary absence")
  val temporaryAbsence: TemporaryAbsenceResponse? = null,

  @Schema(description = "Temporary absence return")
  val temporaryAbsenceReturn: TemporaryAbsenceReturnResponse? = null,
)

@Schema(description = "Scheduled temporary absence response")
data class ScheduledTemporaryAbsenceResponse(
  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Event date")
  val eventDate: LocalDate?,

  @Schema(description = "Start time")
  val startTime: LocalDateTime?,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Escort")
  val escort: String,

  @Schema(description = "From prison")
  val fromPrison: String?,

  @Schema(description = "To agency")
  val toAgency: String?,

  @Schema(description = "Transport type")
  val transportType: String,

  @Schema(description = "Return date")
  val returnDate: LocalDate,

  @Schema(description = "Return time")
  val returnTime: LocalDateTime,

  @Schema(description = "To address ID")
  val toAddressId: Long?,

  @Schema(description = "To address owner class")
  val toAddressOwnerClass: String?,

  @Schema(description = "Application date")
  val applicationDate: LocalDateTime,

  @Schema(description = "Application time")
  val applicationTime: LocalDateTime?,
)

@Schema(description = "Scheduled temporary absence return response")
data class ScheduledTemporaryAbsenceReturnResponse(
  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Event date")
  val eventDate: LocalDate?,

  @Schema(description = "Start time")
  val startTime: LocalDateTime?,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Escort")
  val escort: String,

  @Schema(description = "From agency")
  val fromAgency: String?,

  @Schema(description = "To prison")
  val toPrison: String?,
)

@Schema(description = "Temporary absence response")
data class TemporaryAbsenceResponse(
  @Schema(description = "Movement sequence")
  val sequence: Long,

  @Schema(description = "Movement date")
  val movementDate: LocalDate,

  @Schema(description = "Movement time")
  val movementTime: LocalDateTime,

  @Schema(description = "Movement reason")
  val movementReason: String,

  @Schema(description = "Arresting Agency")
  val arrestAgency: String?,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "Escort text")
  val escortText: String?,

  @Schema(description = "From prison")
  val fromPrison: String?,

  @Schema(description = "To agency")
  val toAgency: String?,

  @Schema(description = "Comment text")
  val commentText: String?,

  @Schema(description = "To address ID")
  val toAddressId: Long?,

  @Schema(description = "To address owner class")
  val toAddressOwnerClass: String?,
)

@Schema(description = "Temporary absence return response")
data class TemporaryAbsenceReturnResponse(
  @Schema(description = "Movement sequence")
  val sequence: Long,

  @Schema(description = "Movement date")
  val movementDate: LocalDate,

  @Schema(description = "Movement time")
  val movementTime: LocalDateTime,

  @Schema(description = "Movement reason")
  val movementReason: String,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "Escort text")
  val escortText: String?,

  @Schema(description = "From agency")
  val fromAgency: String?,

  @Schema(description = "To prison")
  val toPrison: String?,

  @Schema(description = "Comment text")
  val commentText: String?,

  @Schema(description = "From address ID")
  val fromAddressId: Long?,

  @Schema(description = "From address owner class")
  val fromAddressOwnerClass: String?,
)
