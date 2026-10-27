package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.roundToNearestSecond
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.movement.PersonLocationMovement
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementDirection
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementType
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PersonLocationMovementResourceIntTest(
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private val now = LocalDateTime.now().roundToNearestSecond()
  private val yesterday = now.minusDays(1)
  private val twoDaysAgo = now.minusDays(2)

  @AfterEach
  fun tearDown() {
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/person-locations/movement/{bookingId}/{movementSeq}")
  inner class GetPersonLocationMovement {

    @Nested
    inner class HappyPath {
      @Test
      fun `should get admission and release details`() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = yesterday) {
              release(date = now, comment = "Release comment")
            }
          }
        }

        with(webTestClient.getPersonLocationMovementOk(sequence = 1)) {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(sequence).isEqualTo(1)
          assertThat(movementType).isEqualTo(MovementType.ADM)
          assertThat(directionCode).isEqualTo(MovementDirection.IN)
          assertThat(movementTime).isEqualTo(yesterday)
          assertThat(movementReason.code).isEqualTo("N")
          assertThat(movementReason.description).isEqualTo("Unconvicted Remand")
          assertThat(from).isEqualTo(MovementLocation("ABDRCT", MovementLocationType.COURT, "court 2"))
          assertThat(to).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
          assertThat(scheduleId).isNull()
          assertThat(commentText).isNull()
          assertThat(audit.createDatetime).isCloseTo(now, within(5, ChronoUnit.MINUTES))
          assertThat(audit.createUsername).isEqualTo("SA")
        }
        with(webTestClient.getPersonLocationMovementOk(sequence = 2)) {
          assertThat(movementType).isEqualTo(MovementType.REL)
          assertThat(directionCode).isEqualTo(MovementDirection.OUT)
          assertThat(movementTime).isEqualTo(now)
          assertThat(movementReason.code).isEqualTo("CR")
          assertThat(from).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
          assertThat(to).isEqualTo(MovementLocation("OUT", MovementLocationType.OTHER, "OUTSIDE"))
          assertThat(commentText).isEqualTo("Release comment")
        }
      }

      @Test
      fun `should get unscheduled TAP details`() {
        nomisDataBuilder.build {
          agencyLocation(agencyLocationId = "NGENHO", description = "Northern General Hospital", type = "HOSPITAL")
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapMovementOut(date = yesterday, toAgency = "NGENHO", comment = "Tap out comment")
              tapMovementIn(date = now, comment = "Tap in comment")
            }
          }
        }

        with(webTestClient.getPersonLocationMovementOk(sequence = 2)) {
          assertThat(movementType).isEqualTo(MovementType.TAP)
          assertThat(directionCode).isEqualTo(MovementDirection.OUT)
          assertThat(movementReason.code).isEqualTo("C5")
          assertThat(from).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
          assertThat(to).isEqualTo(MovementLocation("NGENHO", MovementLocationType.OTHER, "Northern General Hospital"))
          assertThat(scheduleId).isNull()
          assertThat(commentText).isEqualTo("Tap out comment")
        }
        with(webTestClient.getPersonLocationMovementOk(sequence = 3)) {
          assertThat(movementType).isEqualTo(MovementType.TAP)
          assertThat(directionCode).isEqualTo(MovementDirection.IN)
          assertThat(from).isEqualTo(MovementLocation("NGENHO", MovementLocationType.OTHER, "Northern General Hospital"))
          assertThat(to).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
          assertThat(commentText).isEqualTo("Tap in comment")
        }
      }

      @Test
      fun `should get court movement details`() {
        lateinit var scheduleOut: CourtEvent
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              scheduleOut = courtScheduleOut {
                courtMovementOut(date = yesterday)
                courtMovementIn(date = now)
              }
            }
          }
        }

        with(webTestClient.getPersonLocationMovementOk(sequence = 2)) {
          assertThat(movementType).isEqualTo(MovementType.CRT)
          assertThat(directionCode).isEqualTo(MovementDirection.OUT)
          assertThat(scheduleId).isEqualTo(scheduleOut.id)
        }
        with(webTestClient.getPersonLocationMovementOk(sequence = 3)) {
          assertThat(movementType).isEqualTo(MovementType.CRT)
          assertThat(directionCode).isEqualTo(MovementDirection.IN)
          assertThat(scheduleId).isNull()
        }
      }

      @Test
      fun `should get schedule ID for scheduled TAP movement`() {
        lateinit var scheduleOut: OffenderTapScheduleOut
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapApplication {
                scheduleOut = tapScheduleOut(fromPrison = "BXI", toAgency = "HAZLWD") {
                  tapMovementOut(date = yesterday)
                }
              }
            }
          }
        }

        with(webTestClient.getPersonLocationMovementOk(sequence = 2)) {
          assertThat(movementType).isEqualTo(MovementType.TAP)
          assertThat(scheduleId).isEqualTo(scheduleOut.eventId)
          assertThat(to?.code).isEqualTo("HAZLWD")
        }
      }

      @Test
      fun `should get transfer movement details`() {
        lateinit var scheduleOut: OffenderTransferScheduleOut
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              scheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "MDI") {
                transferMovementOut(date = yesterday)
              }
              receive(date = now)
            }
          }
        }

        with(webTestClient.getPersonLocationMovementOk(sequence = 2)) {
          assertThat(movementType).isEqualTo(MovementType.TRN)
          assertThat(directionCode).isEqualTo(MovementDirection.OUT)
          assertThat(scheduleId).isEqualTo(scheduleOut.eventId)
          assertThat(from?.code).isEqualTo("BXI")
          assertThat(to?.code).isEqualTo("MDI")
        }
        with(webTestClient.getPersonLocationMovementOk(sequence = 3)) {
          assertThat(movementType).isEqualTo(MovementType.ADM)
          assertThat(directionCode).isEqualTo(MovementDirection.IN)
          assertThat(scheduleId).isNull()
        }
      }

      @Test
      fun `should return unknown from location for a release with no from location`() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              release(date = now)
            }
          }
        }
        updateMovements("FROM_AGY_LOC_ID = null", "MOVEMENT_TYPE = 'REL'")

        assertThat(webTestClient.getPersonLocationMovementOk(sequence = 2).from)
          .isEqualTo(MovementLocation("UNKNOWN", MovementLocationType.OTHER, "Unknown"))
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found for unknown offender`() {
        nomisDataBuilder.build { offender = offender { booking = booking() } }

        webTestClient.getPersonLocationMovement(offenderNo = "A9999ZZ", sequence = 1)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found for unknown booking`() {
        nomisDataBuilder.build { offender = offender { booking = booking() } }

        webTestClient.getPersonLocationMovement(bookingId = 999999, sequence = 1)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found for unknown movement sequence`() {
        nomisDataBuilder.build { offender = offender { booking = booking() } }

        webTestClient.getPersonLocationMovement(sequence = 99)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found for a booking belonging to another offender`() {
        lateinit var otherBooking: OffenderBooking
        nomisDataBuilder.build {
          offender = offender { booking = booking() }
          offender(nomsId = "A1234TT") { otherBooking = booking() }
        }

        webTestClient.getPersonLocationMovement(bookingId = otherBooking.bookingId, sequence = 1)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found for a movement created during a merge`() {
        nomisDataBuilder.build { offender = offender { booking = booking(bookingBeginDate = twoDaysAgo) { release(date = now) } } }
        updateMovements("CREATE_USER_ID = 'SYS', AUDIT_MODULE_NAME = 'MERGE'", "MOVEMENT_SEQ = 2")

        webTestClient.getPersonLocationMovement(sequence = 2).expectStatus().isNotFound
        webTestClient.getPersonLocationMovement(sequence = 1).expectStatus().isOk
      }

      @Test
      fun `should return not found for a movement with a null direction`() {
        nomisDataBuilder.build { offender = offender { booking = booking(bookingBeginDate = twoDaysAgo) { release(date = now) } } }
        updateMovements("DIRECTION_CODE = null", "MOVEMENT_TYPE = 'REL'")

        webTestClient.getPersonLocationMovement(sequence = 2).expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/A1234BC/person-locations/movement/12345/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/person-locations/movement/12345/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/person-locations/movement/12345/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  private fun updateMovements(set: String, where: String) = repository.runInTransaction {
    entityManager.createNativeQuery(
      "update OFFENDER_EXTERNAL_MOVEMENTS set $set where OFFENDER_BOOK_ID = ${booking.bookingId} and $where",
    ).executeUpdate()
  }

  private fun WebTestClient.getPersonLocationMovement(
    offenderNo: String = offender.nomsId,
    bookingId: Long = booking.bookingId,
    sequence: Int,
  ) = get()
    .uri("/movements/$offenderNo/person-locations/movement/$bookingId/$sequence")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getPersonLocationMovementOk(sequence: Int) = getPersonLocationMovement(sequence = sequence)
    .expectStatus().isOk
    .expectBodyResponse<PersonLocationMovement>()
}
