package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule.CourtScheduleOut
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class CourtScheduleResourceIntTest : IntegrationTestBase() {

  private val offenderNo = "B7463BB"
  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var scheduleOut: CourtEvent
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
  @DisplayName("GET /movements/{offenderNo}/court/schedule/out/{eventId}")
  inner class GetCourtScheduleOut {

    @Nested
    inner class HappyPath {
      @BeforeEach
      fun setUp() {
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
      }

      @Test
      fun `should get all court schedule details`() {
        webTestClient.getCourtScheduleOutOk(offenderNo, scheduleOut.id)
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(eventId).isEqualTo(scheduleOut.id)
            assertThat(eventDate).isEqualTo(scheduleOut.eventDate)
            assertThat(startTime).isEqualTo(scheduleOut.startTime)
            assertThat(eventType).isEqualTo(scheduleOut.courtEventType.code)
            assertThat(eventStatus).isEqualTo(scheduleOut.eventStatus.code)
            assertThat(comment).isEqualTo(scheduleOut.commentText)
            assertThat(prison).isEqualTo(booking.location.id)
            assertThat(courtCaseId).isEqualTo(courtCase.id)
          }
      }

      @Test
      fun `should allow missing court case`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEvent()
            }
          }
        }

        webTestClient.getCourtScheduleOutOk(offenderNo, scheduleOut.id)
          .apply {
            assertThat(courtCaseId).isNull()
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(eventId).isEqualTo(scheduleOut.id)
            assertThat(eventDate).isEqualTo(scheduleOut.eventDate)
            assertThat(startTime).isEqualTo(scheduleOut.startTime)
            assertThat(eventType).isEqualTo(scheduleOut.courtEventType.code)
            assertThat(eventStatus).isEqualTo(scheduleOut.eventStatus.code)
            assertThat(comment).isEqualTo(scheduleOut.commentText)
            assertThat(prison).isEqualTo(booking.location.id)
            assertThat(audit.createUsername).isEqualTo("SA")
            assertThat(audit.createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
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

        webTestClient.getCourtScheduleOutOk(offenderNo, scheduleOut.id)
          .apply {
            assertThat(prison).isEqualTo("MDI")
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.getCourtScheduleOut(offenderNo = "UNKNOWN", eventId = 1)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found if offender event doesn't exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEvent()
            }
          }
        }

        webTestClient.getCourtScheduleOut(offenderNo = offenderNo, eventId = 9999)
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
          .uri("/movements/$offenderNo/court/schedule/out/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/schedule/out/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/court/schedule/out/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getCourtScheduleOut(offenderNo: String = offender.nomsId, eventId: Long = scheduleOut.id) = get()
      .uri("/movements/$offenderNo/court/schedule/out/$eventId")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()

    private fun WebTestClient.getCourtScheduleOutOk(offenderNo: String = offender.nomsId, eventId: Long = scheduleOut.id) = getCourtScheduleOut(offenderNo, eventId)
      .expectStatus().isOk
      .expectBodyResponse<CourtScheduleOut>()
  }
}
