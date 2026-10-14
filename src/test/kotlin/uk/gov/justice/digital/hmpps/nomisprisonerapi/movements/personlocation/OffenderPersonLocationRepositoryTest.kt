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
          courtMovementOut(
            date = LocalDateTime.parse("2026-01-02T10:00:00"),
            fromPrison = "BXI",
            toCourt = "ABDRCT",
          )
          courtMovementIn(
            date = LocalDateTime.parse("2026-01-02T17:00:00"),
            fromCourt = "ABDRCT",
            toPrison = "BXI",
          )
          tapMovementOut(
            date = LocalDateTime.parse("2026-01-03T08:00:00"),
            fromPrison = "BXI",
            toAgency = "HAZLWD",
          )
          tapMovementIn(
            date = LocalDateTime.parse("2026-01-03T15:00:00"),
            fromAgency = "HAZLWD",
            toPrison = "BXI",
          )
          transferMovementOut(
            date = LocalDateTime.parse("2026-01-04T12:00:00"),
            fromPrison = "BXI",
            toPrison = "MDI",
          )
          receive(
            date = LocalDateTime.parse("2026-01-04T18:00:00"),
            fromAgency = "BXI",
            toPrison = "MDI",
          )
          release(
            date = LocalDateTime.parse("2026-01-05T12:00:00"),
          )
        }
      }
    }

    repository.runInTransaction {
      with(externalMovementRepository.findAllById_OffenderBooking_Offender_NomsId(offender.nomsId)) {
        assertThat(this).hasSize(8)
        with(find { it.movementReason.id.type == "ADM" && it.toAgency!!.id == "BXI" }!!) {
          assertThat(id.sequence).isEqualTo(1)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-01T12:00:00")
          assertThat(movementDirection).isEqualTo(IN)
          assertThat(fromAgency!!.id).isEqualTo("ABDRCT")
        }
        with(find { it.movementReason.id.type == "CRT" && it.movementDirection == OUT }!!) {
          assertThat(id.sequence).isEqualTo(2)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-02T10:00:00")
          assertThat(fromAgency!!.id).isEqualTo("BXI")
          assertThat(toAgency!!.id).isEqualTo("ABDRCT")
        }
        with(find { it.movementReason.id.type == "CRT" && it.movementDirection == IN }!!) {
          assertThat(id.sequence).isEqualTo(3)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-02T17:00:00")
          assertThat(fromAgency!!.id).isEqualTo("ABDRCT")
          assertThat(toAgency!!.id).isEqualTo("BXI")
        }
        with(find { it.movementReason.id.type == "TAP" && it.movementDirection == OUT }!!) {
          assertThat(id.sequence).isEqualTo(4)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-03T08:00:00")
          assertThat(fromAgency!!.id).isEqualTo("BXI")
          assertThat(toAgency!!.id).isEqualTo("HAZLWD")
        }
        with(find { it.movementReason.id.type == "TAP" && it.movementDirection == IN }!!) {
          assertThat(id.sequence).isEqualTo(5)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-03T15:00:00")
          assertThat(fromAgency!!.id).isEqualTo("HAZLWD")
          assertThat(toAgency!!.id).isEqualTo("BXI")
        }
        with(find { it.movementReason.id.type == "TRN" }!!) {
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
        with(find { it.movementReason.id.type == "REL" }!!) {
          assertThat(id.sequence).isEqualTo(8)
          assertThat(getMovementDateAndTime()).isEqualTo("2026-01-05T12:00:00")
          assertThat(fromAgency!!.id).isEqualTo("MDI")
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
          transferMovementOut(
            date = LocalDateTime.parse("2026-01-02T17:00:00"),
            toPrison = "MDI",
          )
          receive(
            date = LocalDateTime.parse("2026-01-02T21:00:00"),
            fromAgency = "BXI",
            toPrison = "MDI",
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
          courtMovementOut(
            date = LocalDateTime.parse("2026-02-02T10:00:00"),
            toCourt = "LEEDYC",
          )
          courtMovementIn(
            date = LocalDateTime.parse("2026-02-02T17:00:00"),
            fromCourt = "LEEDYC",
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
            tapScheduleOut {
              tapMovementOut(
                date = LocalDateTime.parse("2026-01-02T09:00"),
                toAgency = "HAZLWD",
              )
              tapScheduleIn {
                tapMovementIn(
                  date = LocalDateTime.parse("2026-01-02T17:00"),
                  fromAgency = "HAZLWD",
                )
              }
            }
          }
          courtScheduleOut {
            courtMovementOut(
              date = LocalDateTime.parse("2026-01-03T10:00"),
              toCourt = "ABDRCT",
            )
            courtMovementIn(
              date = LocalDateTime.parse("2026-01-03T17:00"),
              fromCourt = "ABDRCT",
            )
          }
          transferScheduleOut {
            transferMovementOut(
              date = LocalDateTime.parse("2026-01-04T12:00"),
              toPrison = "MDI",
            )
          }
          receive(
            date = LocalDateTime.parse("2026-01-04T18:00"),
            fromAgency = "BXI",
            toPrison = "MDI",
          )
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
}
