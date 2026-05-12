package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.offender.OffenderCourtMovementsResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class OffenderCourtMovementsResourceIntTest : IntegrationTestBase() {

  private val offenderNo = "C7463CC"
  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var scheduleOut: CourtEvent
  private lateinit var movementOut: OffenderCourtMovementOut
  private lateinit var movementIn: OffenderCourtMovementIn
  private lateinit var staff: Staff
  private lateinit var courtCase: CourtCase

  @AfterEach
  fun tearDown() {
    if (::scheduleOut.isInitialized) {
      repository.delete(scheduleOut)
    }
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/court")
  inner class GetOffenderCourtMovements {

    @Nested
    inner class HappyPath {
      @Test
      fun `should get court schedule`() {
        nomisDataBuilder.build {
          staff = staff {
            account()
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              courtCase = courtCase(reportingStaff = staff) {
                scheduleOut = courtEvent()
              }
            }
          }
        }

        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            assertThat(bookings).hasSize(1)
            assertThat(bookings[0].bookingId).isEqualTo(booking.bookingId)
            assertThat(bookings[0].courtSchedules).hasSize(1)
            with(bookings[0].courtSchedules[0]) {
              assertThat(eventId).isEqualTo(scheduleOut.id)
              assertThat(eventDate).isEqualTo(scheduleOut.eventDate)
              assertThat(startTime).isEqualTo(scheduleOut.startTime)
              assertThat(eventType).isEqualTo(scheduleOut.courtEventType.code)
              assertThat(eventStatus).isEqualTo(scheduleOut.eventStatus.code)
              assertThat(comment).isEqualTo(scheduleOut.commentText)
              assertThat(prison).isEqualTo(booking.location.id)
              assertThat(court).isEqualTo(scheduleOut.court.id)
              assertThat(courtCaseId).isEqualTo(courtCase.id)
              assertThat(audit.createUsername).isEqualTo("SA")
              assertThat(audit.createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
            }
          }
      }

      @Test
      fun `should get court case without court schedule`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEvent()
            }
          }
        }

        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            assertThat(bookings[0].courtSchedules).hasSize(1)
            with(bookings[0].courtSchedules[0]) {
              assertThat(eventId).isEqualTo(scheduleOut.id)
              assertThat(courtCaseId).isNull()
            }
          }
      }

      @Test
      fun `should get prison from offender's prison at time of schedule creation`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking(agencyLocationId = "MDI", bookingBeginDate = LocalDateTime.now().minusDays(1)) {
              // The court schedule was created while in MDI
              scheduleOut = courtEvent(whenCreated = LocalDateTime.now().minusHours(6))
              prisonTransfer(from = "MDI", to = "BXI", date = LocalDateTime.now().minusHours(1))
            }
          }
        }

        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0].courtSchedules[0]) {
              assertThat(prison).isEqualTo("MDI")
            }
          }
      }

      @Test
      fun `should get court movement OUT`() {
        nomisDataBuilder.build {
          staff = staff {
            account()
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              courtCase = courtCase(reportingStaff = staff) {
                scheduleOut = courtEvent {
                  movementOut = courtMovementOut()
                }
              }
            }
          }
        }

        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0].courtSchedules[0].courtMovementOut!!) {
              assertThat(sequence).isEqualTo(movementOut.id.sequence)
              assertThat(movementDate).isEqualTo(movementOut.movementDate)
              assertThat(movementTime).isCloseTo(movementOut.movementTime, within(Duration.ofSeconds(1)))
              assertThat(movementReason).isEqualTo(movementOut.movementReason.id.reasonCode)
              assertThat(fromPrison).isEqualTo("BXI")
              assertThat(toCourt).isEqualTo(movementOut.toAgency!!.id)
              assertThat(commentText).isEqualTo(movementOut.commentText)
              assertThat(audit.createUsername).isEqualTo("SA")
              assertThat(audit.createDatetime).isCloseTo(movementOut.createDatetime, within(10, SECONDS))
            }
          }
      }

      @Test
      fun `should get court movement OUT without court case`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEvent {
                movementOut = courtMovementOut()
              }
            }
          }
        }

        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0].courtSchedules[0].courtMovementOut!!) {
              assertThat(sequence).isEqualTo(movementOut.id.sequence)
            }
          }
      }

      @Test
      fun `should get court movement OUT without schedule`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              movementOut = courtMovementOut()
            }
          }
        }

        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0].unscheduledCourtMovementOuts[0]) {
              assertThat(sequence).isEqualTo(movementOut.id.sequence)
            }
          }
      }

      @Test
      fun `should get court movement IN`() {
        nomisDataBuilder.build {
          staff = staff {
            account()
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              courtCase = courtCase(reportingStaff = staff) {
                scheduleOut = courtEvent {
                  movementOut = courtMovementOut()
                  movementIn = courtMovementIn()
                }
              }
            }
          }
        }

        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0].courtSchedules[0].courtMovementIn!!) {
              assertThat(sequence).isEqualTo(movementIn.id.sequence)
              assertThat(movementDate).isEqualTo(movementIn.movementDate)
              assertThat(movementTime).isCloseTo(movementIn.movementTime, within(Duration.ofSeconds(1)))
              assertThat(movementReason).isEqualTo(movementIn.movementReason.id.reasonCode)
              assertThat(toPrison).isEqualTo(movementIn.toAgency!!.id)
              assertThat(fromCourt).isEqualTo(movementIn.fromAgency!!.id)
              assertThat(commentText).isEqualTo(movementIn.commentText)
              assertThat(audit.createUsername).isEqualTo("SA")
              assertThat(audit.createDatetime).isCloseTo(movementIn.createDatetime, within(10, SECONDS))
            }
          }
      }

      @Test
      fun `should get court movement IN without court case`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEvent {
                movementOut = courtMovementOut()
                movementIn = courtMovementIn()
              }
            }
          }
        }

        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0].courtSchedules[0].courtMovementIn!!) {
              assertThat(sequence).isEqualTo(movementIn.id.sequence)
            }
          }
      }

      @Test
      fun `should get court movement IN without schedule`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              movementOut = courtMovementOut()
              movementIn = courtMovementIn()
            }
          }
        }

        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0].unscheduledCourtMovementIns[0]) {
              assertThat(sequence).isEqualTo(movementIn.id.sequence)
            }
          }
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/A1234BC/court")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/court")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/court")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  private fun WebTestClient.getOffenderCourtMovements(offenderNo: String = offender.nomsId) = get()
    .uri("/movements/$offenderNo/court")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getOffenderCourtMovementsOk(offenderNo: String = offender.nomsId) = getOffenderCourtMovements(offenderNo)
    .expectStatus().isOk
    .expectBodyResponse<OffenderCourtMovementsResponse>()
}
