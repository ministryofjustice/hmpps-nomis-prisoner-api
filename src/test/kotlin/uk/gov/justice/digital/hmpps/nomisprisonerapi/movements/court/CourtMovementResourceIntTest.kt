package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCourtMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.movement.CourtMovementOut
import java.time.Duration
import java.time.temporal.ChronoUnit

class CourtMovementResourceIntTest : IntegrationTestBase() {

  private val offenderNo = "B7463BB"
  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var movementOut: OffenderCourtMovementOut
  private lateinit var movementIn: OffenderCourtMovementIn
  private lateinit var scheduleOut: CourtEvent
  private lateinit var staff: Staff

  @Nested
  @DisplayName("GET /movements/{offenderNo}/court/movement/out/{bookingId}/{movementSeq}")
  inner class GetCourtMovementOut {

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff = staff {
          account()
        }
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            scheduleOut = courtEvent {
              movementOut = courtMovementOut()
              movementIn = courtMovementIn()
            }
          }
        }
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `should get all court movement details`() {
        webTestClient.getCourtMovementOutOk().apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(sequence).isEqualTo(movementOut.id.sequence)
          assertThat(courtScheduleOutId).isEqualTo(scheduleOut.id)
          assertThat(movementDate).isEqualTo(movementOut.movementDate)
          assertThat(movementTime).isCloseTo(movementOut.movementTime, within(Duration.ofSeconds(1)))
          assertThat(movementReason).isEqualTo(movementOut.movementReason.id.reasonCode)
          assertThat(fromPrison).isEqualTo(booking.location.id)
          assertThat(toCourt).isEqualTo(movementOut.toAgency!!.id)
          assertThat(commentText).isEqualTo(movementOut.commentText)
          assertThat(audit.createUsername).isEqualTo("SA")
          assertThat(audit.createDatetime).isCloseTo(movementOut.createDatetime, within(10, ChronoUnit.SECONDS))
        }
      }

      @Test
      fun `should get unscheduled court movement details`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              movementOut = courtMovementOut()
              movementIn = courtMovementIn()
            }
          }
        }

        webTestClient.getCourtMovementOutOk().apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(sequence).isEqualTo(movementOut.id.sequence)
          assertThat(courtScheduleOutId).isNull()
        }
      }

      @Test
      fun `should get court movement details linked to a court case`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              courtCase(reportingStaff = staff) {
                scheduleOut = courtEvent {
                  movementOut = courtMovementOut()
                  movementIn = courtMovementIn()
                }
              }
            }
          }
        }

        webTestClient.getCourtMovementOutOk().apply {
          assertThat(bookingId).isEqualTo(booking.bookingId)
          assertThat(sequence).isEqualTo(movementOut.id.sequence)
          assertThat(courtScheduleOutId).isEqualTo(scheduleOut.id)
        }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.getCourtMovementOut(offenderNo = "UNKNOWN")
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found if booking doesn't exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              movementOut = courtMovementOut()
              movementIn = courtMovementIn()
            }
          }
        }

        webTestClient.getCourtMovementOut(offenderNo = offender.nomsId, bookingId = 9999, sequence = 1)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found if court movement doesn't exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              movementOut = courtMovementOut()
              movementIn = courtMovementIn()
            }
          }
        }

        webTestClient.getCourtMovementOut(offenderNo = offender.nomsId, bookingId = booking.bookingId, sequence = 9999)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/out/12345/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/out/12345/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/out/12345/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/court/movement/in/{bookingId}/{movementSeq}")
  inner class GetCourtMovementIn {

    @Nested
    inner class Security {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/in/12345/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/in/12345/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/movement/in/12345/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  private fun WebTestClient.getCourtMovementOut(offenderNo: String = offender.nomsId, bookingId: Long = movementOut.id.offenderBooking.bookingId, sequence: Int = movementOut.id.sequence) = get()
    .uri("/movements/$offenderNo/court/movement/out/$bookingId/$sequence")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getCourtMovementOutOk(offenderNo: String = offender.nomsId, bookingId: Long = movementOut.id.offenderBooking.bookingId, sequence: Int = movementOut.id.sequence) = getCourtMovementOut(offenderNo, bookingId, sequence)
    .expectStatus().isOk
    .expectBodyResponse<CourtMovementOut>()
}
