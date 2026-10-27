package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.booking.PersonLocationBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.movement.PersonLocationMovement

@Schema(description = "Offender person locations, with bookings and movements")
data class OffenderPersonLocationsResponse(
  @Schema(description = "List of bookings with their external movements")
  val bookings: List<BookingPersonLocations>,
)

@Schema(description = "Booking person locations")
data class BookingPersonLocations(
  @Schema(description = "The booking")
  val booking: PersonLocationBooking,

  @Schema(description = "Movements related to the booking")
  val movements: List<PersonLocationMovement>,
)

enum class BookingStatus { CLOSED, OPEN }
enum class MovementDirection { IN, OUT }
enum class MovementType { ADM, CRT, TAP, TRN, REL }
enum class MovementLocationType { PRISON, COURT, OTHER }

data class MovementLocation(
  val code: String? = null,
  val type: MovementLocationType,
  val description: String,
)
