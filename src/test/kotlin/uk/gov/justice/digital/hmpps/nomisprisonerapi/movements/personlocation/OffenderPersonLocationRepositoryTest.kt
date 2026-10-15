package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection.IN
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.MovementDirection.OUT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderExternalMovementRepository
import java.time.LocalDateTime

class OffenderPersonLocationRepositoryTest(
  @Autowired private val externalMovementRepository: OffenderExternalMovementRepository,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking

  @AfterEach
  fun `reset data`() {
    repository.deleteOffenders()
  }

  @Test
  fun `should save and load all movement types`() {
    nomisDataBuilder.build {
      offender = offender {
        booking = booking(
          bookingBeginDate = LocalDateTime.parse("2026-01-01T12:00:00"),
          agencyLocationId = "BXI",
        ) {
          courtMovement(
            court = "ABDRCT",
            date = LocalDateTime.parse("2026-01-02T10:00:00"),
            returnDate = LocalDateTime.parse("2026-01-02T17:00:00"),
          )
          tapMovement(
            toAgency = "HAZLWD",
            date = LocalDateTime.parse("2026-01-03T08:00:00"),
            returnDate = LocalDateTime.parse("2026-01-03T15:00:00"),
          )
          prisonTransfer(
            to = "MDI",
            date = LocalDateTime.parse("2026-01-04T12:00:00"),
            arrivalDate = LocalDateTime.parse("2026-01-04T18:00:00"),
          )
          courtTransfer(
            court = "LEEDYC",
            toPrison = "SYI",
            date = LocalDateTime.parse("2026-01-05T12:00:00"),
            arrivalDate = LocalDateTime.parse("2026-01-05T18:00:00"),
          )
          tapTransfer(
            toAgency = "ARNOLD",
            toPrison = "RNI",
            date = LocalDateTime.parse("2026-01-06T12:00:00"),
            arrivalDate = LocalDateTime.parse("2026-01-06T18:00:00"),
          )
          release(
            date = LocalDateTime.parse("2026-01-07T11:00:00"),
          )
        }
      }
    }

    repository.runInTransaction {
      with(externalMovementRepository.findAllById_OffenderBooking_Offender_NomsId(offender.nomsId)) {
        assertThat(this).hasSize(12)
        with(find { it.movementReason.id.type == "ADM" && it.toAgency!!.id == "BXI" }!!) {
          assertThat(id.sequence).isEqualTo(1)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-01T12:00:00")
          assertThat(movementDirection).isEqualTo(IN)
          assertThat(fromAgency!!.id).isEqualTo("ABDRCT")
        }
        with(find { it.movementReason.id.type == "CRT" && it.toAgency!!.id == "ABDRCT" }!!) {
          assertThat(id.sequence).isEqualTo(2)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-02T10:00:00")
          assertThat(fromAgency!!.id).isEqualTo("BXI")
          assertThat(toAgency!!.id).isEqualTo("ABDRCT")
        }
        with(find { it.movementReason.id.type == "CRT" && it.toAgency!!.id == "BXI" }!!) {
          assertThat(id.sequence).isEqualTo(3)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-02T17:00:00")
          assertThat(fromAgency!!.id).isEqualTo("ABDRCT")
          assertThat(toAgency!!.id).isEqualTo("BXI")
        }
        with(find { it.movementReason.id.type == "TAP" && it.toAgency!!.id == "HAZLWD" }!!) {
          assertThat(id.sequence).isEqualTo(4)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-03T08:00:00")
          assertThat(fromAgency!!.id).isEqualTo("BXI")
          assertThat(toAgency!!.id).isEqualTo("HAZLWD")
        }
        with(find { it.movementReason.id.type == "TAP" && it.toAgency!!.id == "BXI" }!!) {
          assertThat(id.sequence).isEqualTo(5)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-03T15:00:00")
          assertThat(fromAgency!!.id).isEqualTo("HAZLWD")
          assertThat(toAgency!!.id).isEqualTo("BXI")
        }
        with(find { it.movementReason.id.type == "TRN" && it.toAgency!!.id == "MDI" }!!) {
          assertThat(id.sequence).isEqualTo(6)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-04T12:00:00")
          assertThat(fromAgency!!.id).isEqualTo("BXI")
          assertThat(toAgency!!.id).isEqualTo("MDI")
        }
        with(find { it.movementReason.id.type == "ADM" && it.toAgency!!.id == "MDI" }!!) {
          assertThat(id.sequence).isEqualTo(7)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-04T18:00:00")
          assertThat(fromAgency!!.id).isEqualTo("BXI")
          assertThat(toAgency!!.id).isEqualTo("MDI")
        }
        with(find { it.movementReason.id.type == "CRT" && it.toAgency!!.id == "LEEDYC" }!!) {
          assertThat(id.sequence).isEqualTo(8)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-05T12:00:00")
          assertThat(fromAgency!!.id).isEqualTo("MDI")
          assertThat(toAgency!!.id).isEqualTo("LEEDYC")
        }
        with(find { it.movementReason.id.type == "ADM" && it.toAgency!!.id == "SYI" }!!) {
          assertThat(id.sequence).isEqualTo(9)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-05T18:00:00")
          assertThat(fromAgency!!.id).isEqualTo("LEEDYC")
          assertThat(movementReason.id.reasonCode).isEqualTo("TRNCRT")
        }
        with(find { it.movementReason.id.type == "TAP" && it.toAgency!!.id == "ARNOLD" }!!) {
          assertThat(id.sequence).isEqualTo(10)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-06T12:00:00")
          assertThat(fromAgency!!.id).isEqualTo("SYI")
        }
        with(find { it.movementReason.id.type == "ADM" && it.toAgency!!.id == "RNI" }!!) {
          assertThat(id.sequence).isEqualTo(11)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-06T18:00:00")
          assertThat(fromAgency!!.id).isEqualTo("ARNOLD")
          assertThat(movementReason.id.reasonCode).isEqualTo("TRNTAP")
        }
        with(find { it.movementReason.id.type == "REL" }!!) {
          assertThat(id.sequence).isEqualTo(12)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-07T11:00:00")
          assertThat(fromAgency!!.id).isEqualTo("RNI")
          assertThat(toAgency!!.id).isEqualTo("OUT")
        }
      }
    }
  }

  @Test
  fun `should save and load multiple bookings`() {
    lateinit var activeBooking: OffenderBooking

    nomisDataBuilder.build {
      offender = offender {
        booking = booking(
          bookingSequence = 1,
          bookingBeginDate = LocalDateTime.parse("2026-01-01T12:00:00"),
          agencyLocationId = "BXI",
        ) {
          prisonTransfer(
            to = "MDI",
            date = LocalDateTime.parse("2026-01-02T17:00:00"),
            arrivalDate = LocalDateTime.parse("2026-01-02T21:00:00"),
          )
          release(
            date = LocalDateTime.parse("2026-01-03T12:00:00"),
          )
        }
        activeBooking = booking(
          bookingSequence = 2,
          bookingBeginDate = LocalDateTime.parse("2026-02-01T11:00:00"),
          agencyLocationId = "LEI",
        ) {
          courtMovement(
            prison = "LEI",
            court = "LEEDYC",
            date = LocalDateTime.parse("2026-02-02T10:00:00"),
            returnDate = LocalDateTime.parse("2026-02-02T17:00:00"),
          )
        }
      }
    }

    repository.runInTransaction {
      with(externalMovementRepository.findAllById_OffenderBooking_Offender_NomsId(offender.nomsId)) {
        assertThat(this).hasSize(7)
        with(find { it.movementReason.id.type == "ADM" && it.toAgency!!.id == "BXI" }!!) {
          assertThat(id.offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(id.sequence).isEqualTo(1)
          assertThat(active).isFalse
        }
        with(find { it.movementReason.id.type == "TRN" && it.movementDirection == OUT }!!) {
          assertThat(id.offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(id.sequence).isEqualTo(2)
          assertThat(active).isFalse
        }
        with(find { it.movementReason.id.type == "ADM" && it.toAgency!!.id == "MDI" }!!) {
          assertThat(id.offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(id.sequence).isEqualTo(3)
          assertThat(active).isFalse
        }
        with(find { it.movementReason.id.type == "REL" }!!) {
          assertThat(id.offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(id.sequence).isEqualTo(4)
          assertThat(active).isTrue
        }
        with(find { it.movementReason.id.type == "ADM" && it.toAgency!!.id == "LEI" }!!) {
          assertThat(id.offenderBooking.bookingId).isEqualTo(activeBooking.bookingId)
          assertThat(id.sequence).isEqualTo(1)
          assertThat(active).isFalse
        }
        with(find { it.movementReason.id.type == "CRT" && it.movementDirection == OUT }!!) {
          assertThat(id.offenderBooking.bookingId).isEqualTo(activeBooking.bookingId)
          assertThat(id.sequence).isEqualTo(2)
          assertThat(active).isFalse
        }
        with(find { it.movementReason.id.type == "CRT" && it.movementDirection == IN }!!) {
          assertThat(id.offenderBooking.bookingId).isEqualTo(activeBooking.bookingId)
          assertThat(id.sequence).isEqualTo(3)
          assertThat(active).isTrue
        }
      }
    }
  }

  @Test
  fun `should load movements with plans and schedules`() {
    nomisDataBuilder.build {
      offender = offender {
        booking = booking(
          bookingSequence = 1,
          bookingBeginDate = LocalDateTime.parse("2026-01-01T12:00:00"),
          agencyLocationId = "BXI",
        ) {
          tapApplication {
            tapScheduleOut(toAgency = "HAZLWD") {
              tapMovementOut(date = LocalDateTime.parse("2026-01-02T09:00"))
              tapScheduleIn {
                tapMovementIn(date = LocalDateTime.parse("2026-01-02T17:00"))
              }
            }
          }
          courtScheduleOut {
            courtMovementOut(date = LocalDateTime.parse("2026-01-03T10:00"))
            courtMovementIn(date = LocalDateTime.parse("2026-01-03T17:00"))
          }
          transferScheduleOut(toPrison = "MDI") {
            transferMovementOut(date = LocalDateTime.parse("2026-01-04T12:00"))
          }
          receive(date = LocalDateTime.parse("2026-01-04T18:00"))
        }
      }
    }

    repository.runInTransaction {
      with(externalMovementRepository.findAllById_OffenderBooking_Offender_NomsId(offender.nomsId)) {
        assertThat(this).hasSize(7)
        with(find { it.movementReason.id.type == "ADM" && it.toAgency!!.id == "BXI" }!!) {
          assertThat(id.sequence).isEqualTo(1)
        }
        with(find { it.movementReason.id.type == "TAP" && it.movementDirection == OUT }!!) {
          assertThat(id.sequence).isEqualTo(2)
        }
        with(find { it.movementReason.id.type == "TAP" && it.movementDirection == IN }!!) {
          assertThat(id.sequence).isEqualTo(3)
        }
        with(find { it.movementReason.id.type == "CRT" && it.movementDirection == OUT }!!) {
          assertThat(id.sequence).isEqualTo(4)
        }
        with(find { it.movementReason.id.type == "CRT" && it.movementDirection == IN }!!) {
          assertThat(id.sequence).isEqualTo(5)
        }
        with(find { it.movementReason.id.type == "TRN" && it.movementDirection == OUT }!!) {
          assertThat(id.sequence).isEqualTo(6)
        }
        with(find { it.movementReason.id.type == "ADM" && it.toAgency!!.id == "MDI" }!!) {
          assertThat(id.sequence).isEqualTo(7)
        }
      }
    }
  }

  @Test
  fun `should only activate the latest movement when movements are created from schedules`() {
    nomisDataBuilder.build {
      offender = offender {
        booking = booking(
          bookingBeginDate = LocalDateTime.parse("2026-01-01T12:00:00"),
          agencyLocationId = "BXI",
        ) {
          tapApplication {
            tapScheduleOut(toAgency = "HAZLWD") {
              tapMovementOut(date = LocalDateTime.parse("2026-01-02T09:00"))
              tapScheduleIn {
                tapMovementIn(date = LocalDateTime.parse("2026-01-02T17:00"))
              }
            }
          }
          courtScheduleOut {
            courtMovementOut(date = LocalDateTime.parse("2026-01-03T10:00"))
            courtMovementIn(date = LocalDateTime.parse("2026-01-03T17:00"))
          }
          transferScheduleOut(toPrison = "MDI") {
            transferMovementOut(date = LocalDateTime.parse("2026-01-04T12:00"))
          }
        }
      }
    }

    repository.runInTransaction {
      with(externalMovementRepository.findAllById_OffenderBooking_Offender_NomsId(offender.nomsId)) {
        assertThat(this).hasSize(6)
        val latest = maxBy { it.id.sequence }
        assertThat(latest.movementReason.id.type).isEqualTo("TRN")
        assertThat(latest.active).isTrue
        assertThat(filter { it.active }).containsExactly(latest)
      }
    }
  }
}
