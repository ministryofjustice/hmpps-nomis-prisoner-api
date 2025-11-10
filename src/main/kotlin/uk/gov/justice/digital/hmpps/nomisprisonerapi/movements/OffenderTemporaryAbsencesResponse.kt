package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.NomisAudit
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Offender temporary absences by booking, including applications and scheduled absences")
data class OffenderTemporaryAbsencesResponse(
  @Schema(description = "List of bookings with their temporary absences and external movements")
  val bookings: List<BookingTemporaryAbsences>,
)

@Schema(description = "Booking temporary absences")
data class BookingTemporaryAbsences(
  @Schema(description = "Booking ID")
  val bookingId: Long,

  @Schema(description = "Temporary absence applications")
  val temporaryAbsenceApplications: List<TemporaryAbsenceApplication>,

  @Schema(description = "Unscheduled temporary absences OUT - those without an application or a schedule")
  val unscheduledTemporaryAbsences: List<TemporaryAbsence>,

  @Schema(description = "Unscheduled temporary absences IN - those without an application or a schedule")
  val unscheduledTemporaryAbsenceReturns: List<TemporaryAbsenceReturn>,
)

@Schema(description = "Temporary absence application response")
data class TemporaryAbsenceApplication(
  @Schema(description = "Movement application ID")
  val movementApplicationId: Long,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Application date")
  val applicationDate: LocalDate,

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
  val prisonId: String,

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

  @Schema(description = "All scheduled temporary absences")
  val absences: List<Absence> = mutableListOf(),

  @Schema(description = "Outside movements")
  val outsideMovements: List<TemporaryAbsenceApplicationOutsideMovement>,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "A single instance of a scheduled temporary absence including return")
data class Absence(

  @Schema(description = "Scheduled temporary absence")
  val scheduledTemporaryAbsence: ScheduledTemporaryAbsence? = null,

  @Schema(description = "Scheduled temporary absence return")
  val scheduledTemporaryAbsenceReturn: ScheduledTemporaryAbsenceReturn? = null,

  @Schema(description = "Temporary absence")
  val temporaryAbsence: TemporaryAbsence? = null,

  @Schema(description = "Temporary absence return")
  val temporaryAbsenceReturn: TemporaryAbsenceReturn? = null,
)

@Schema(description = "Temporary absence application outside movement response")
data class TemporaryAbsenceApplicationOutsideMovement(
  @Schema(description = "Movement application ID")
  val outsideMovementId: Long,

  @Schema(description = "Temporary absence type")
  val temporaryAbsenceType: String?,

  @Schema(description = "Temporary absence sub type")
  val temporaryAbsenceSubType: String?,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "From date")
  val fromDate: LocalDate,

  @Schema(description = "Release time")
  val releaseTime: LocalDateTime,

  @Schema(description = "To date")
  val toDate: LocalDate,

  @Schema(description = "Return time")
  val returnTime: LocalDateTime,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "To agency ID")
  val toAgencyId: String?,

  @Schema(description = "To address ID")
  val toAddressId: Long?,

  @Schema(description = "To address owner class")
  val toAddressOwnerClass: String?,

  @Schema(description = "Contact person name")
  val contactPersonName: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Scheduled temporary absence response")
data class ScheduledTemporaryAbsence(
  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Event date")
  val eventDate: LocalDate,

  @Schema(description = "Start time")
  val startTime: LocalDateTime,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "From prison")
  val fromPrison: String?,

  @Schema(description = "To agency")
  val toAgency: String?,

  @Schema(description = "Transport type")
  val transportType: String?,

  @Schema(description = "Return date")
  val returnDate: LocalDate,

  @Schema(description = "Return time")
  val returnTime: LocalDateTime,

  @Schema(description = "To address ID")
  val toAddressId: Long?,

  @Schema(description = "To address owner class")
  val toAddressOwnerClass: String?,

  @Schema(description = "To address description")
  val toAddressDescription: String?,

  @Schema(description = "To full address")
  val toFullAddress: String?,

  @Schema(description = "TO address postcode")
  val toAddressPostcode: String?,

  @Schema(description = "Application date")
  val applicationDate: LocalDateTime,

  @Schema(description = "Application time")
  val applicationTime: LocalDateTime?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Scheduled temporary absence return response")
data class ScheduledTemporaryAbsenceReturn(
  @Schema(description = "Event ID")
  val eventId: Long,

  @Schema(description = "Event date")
  val eventDate: LocalDate,

  @Schema(description = "Start time")
  val startTime: LocalDateTime,

  @Schema(description = "Event sub type")
  val eventSubType: String,

  @Schema(description = "Event status")
  val eventStatus: String,

  @Schema(description = "Comment")
  val comment: String?,

  @Schema(description = "Escort")
  val escort: String?,

  @Schema(description = "From agency")
  val fromAgency: String?,

  @Schema(description = "To prison")
  val toPrison: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Temporary absence response")
data class TemporaryAbsence(
  @Schema(description = "Movement sequence")
  val sequence: Int,

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

  @Schema(description = "To address description")
  val toAddressDescription: String?,

  @Schema(description = "Full to address")
  val toFullAddress: String?,

  @Schema(description = "To address postcode")
  val toAddressPostcode: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)

@Schema(description = "Temporary absence return response")
data class TemporaryAbsenceReturn(
  @Schema(description = "Movement sequence")
  val sequence: Int,

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

  @Schema(description = "From address description")
  val fromAddressDescription: String?,

  @Schema(description = "From full address")
  val fromFullAddress: String?,

  @Schema(description = "From address postcode")
  val fromAddressPostcode: String?,

  @Schema(description = "Audit data associated with the records")
  val audit: NomisAudit,
)
