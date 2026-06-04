package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.CourtEventRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule.CourtScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule.UpsertCourtScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court.schedule.UpsertCourtScheduleOutResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class CourtScheduleResourceIntTest(
  @Autowired private val courtEventRepository: CourtEventRepository,
) : IntegrationTestBase() {

  private val offenderNo = "B7463BB"
  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var scheduleOut: CourtEvent
  private lateinit var scheduleIn: CourtEvent
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
            assertThat(court).isEqualTo(scheduleOut.court.id)
            assertThat(courtCaseId).isEqualTo(courtCase.id)
            assertThat(audit.createUsername).isEqualTo("SA")
            assertThat(audit.createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
            assertThat(userActiveCaseloadId).isEqualTo("CADM_I")
          }
      }

      @Test
      fun `should allow missing court case`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut()
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
            assertThat(userActiveCaseloadId).isEqualTo("CADM_I")
          }
      }

      @Test
      fun `should get prison from offender's prison at time of schedule creation`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking(agencyLocationId = "MDI", bookingBeginDate = LocalDateTime.now().minusDays(1)) {
              // The court schedule was created while in MDI
              scheduleOut = courtEventOut(whenCreated = LocalDateTime.now().minusHours(6))
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
              scheduleOut = courtEventOut()
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

  @Nested
  @DisplayName("PUT /movements/{offenderNo}/court/schedule/out")
  inner class PutCourtScheduleOut {
    private val today = LocalDateTime.now()

    @Nested
    inner class Create {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should create court schedule out`() {
        webTestClient.upsertCourtScheduleOutOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(courtEventRepository.findByIdOrNull(eventId)!!) {
                assertThat(startTime).isCloseTo(today, within(1, SECONDS))
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(courtEventType.code).isEqualTo("CRT")
                assertThat(commentText).isEqualTo("court schedule out comment")
                assertThat(court.id).isEqualTo("LEEDYC")
                assertThat(directionCode?.code).isEqualTo("OUT")
              }
              assertThat(courtEventRepository.findByOffenderBooking_BookingIdAndParentEventId(bookingId, eventId)).isNull()
            }
          }
      }

      @Test
      fun `should create court schedule out and in`() {
        webTestClient.upsertCourtScheduleOutOk(
          request = aRequest(
            eventId = null,
            eventStatus = "COMP",
            returnStatus = "SCH",
          ),
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(courtEventRepository.findByIdOrNull(eventId)!!) {
                assertThat(eventStatus.code).isEqualTo("COMP")
                assertThat(directionCode?.code).isEqualTo("OUT")
              }
              with(courtEventRepository.findByOffenderBooking_BookingIdAndParentEventId(bookingId, eventId)!!) {
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(directionCode?.code).isEqualTo("IN")
                assertThat(startTime).isCloseTo(today.withHour(17).withMinute(0).withSecond(0), within(1, SECONDS))
                assertThat(courtEventType.code).isEqualTo("CRT")
                assertThat(commentText).isEqualTo("court schedule out comment")
                assertThat(court.id).isEqualTo("BXI")
              }
            }
          }
      }
    }

    @Nested
    inner class Update {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut()
            }
          }
        }
      }

      @Test
      fun `should update court schedule out`() {
        webTestClient.upsertCourtScheduleOutOk(
          request = aRequest(eventId = scheduleOut.id),
        )
          .apply {
            repository.runInTransaction {
              with(courtEventRepository.findByIdOrNull(eventId)!!) {
                assertThat(courtEventType.code).isEqualTo("CRT")
                assertThat(directionCode?.code).isEqualTo("OUT")
              }
              assertThat(courtEventRepository.findByOffenderBooking_BookingIdAndParentEventId(bookingId, eventId)).isNull()
            }
          }
      }

      @Test
      fun `should create court schedule in`() {
        webTestClient.upsertCourtScheduleOutOk(
          request = aRequest(eventId = scheduleOut.id, eventStatus = "COMP", returnStatus = "SCH"),
        )
          .apply {
            repository.runInTransaction {
              with(courtEventRepository.findByIdOrNull(eventId)!!) {
                assertThat(eventStatus.code).isEqualTo("COMP")
                assertThat(directionCode?.code).isEqualTo("OUT")
              }
              with(courtEventRepository.findByOffenderBooking_BookingIdAndParentEventId(bookingId, eventId)!!) {
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(directionCode?.code).isEqualTo("IN")
              }
            }
          }
      }
    }

    @Nested
    inner class UpdateWithExistingScheduleIn {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut()
              scheduleIn = courtEventOut()
            }
          }
        }
      }

      @Test
      fun `should update court schedule out and in`() {
        webTestClient.upsertCourtScheduleOutOk(
          request = aRequest(eventId = scheduleOut.id, eventStatus = "COMP", returnStatus = "COMP"),
        )
          .apply {
            repository.runInTransaction {
              with(courtEventRepository.findByIdOrNull(eventId)!!) {
                assertThat(eventStatus.code).isEqualTo("COMP")
                assertThat(directionCode?.code).isEqualTo("OUT")
              }
              with(courtEventRepository.findByOffenderBooking_BookingIdAndParentEventId(bookingId, eventId)!!) {
                assertThat(eventStatus.code).isEqualTo("COMP")
                assertThat(directionCode?.code).isEqualTo("IN")
              }
            }
          }
      }

      @Test
      fun `should delete court schedule in`() {
        webTestClient.upsertCourtScheduleOutOk(
          request = aRequest(eventId = scheduleOut.id, returnStatus = null),
        )
          .apply {
            repository.runInTransaction {
              assertThat(courtEventRepository.findByOffenderBooking_BookingIdAndParentEventId(bookingId, eventId)).isNull()
            }
          }
      }
    }

    @Nested
    inner class Validation {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.upsertCourtScheduleOut(offenderNo = "UNKNOWN")
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN").contains("not found")
          }
      }

      @Test
      fun `should return not found if offender has no bookings`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "C1234DE")
        }

        webTestClient.upsertCourtScheduleOut()
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("C1234DE").contains("not found")
          }
      }

      @Test
      fun `should return bad request if event type invalid`() {
        webTestClient.upsertCourtScheduleOut(request = aRequest(eventType = "UNKNOWN"))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN")
          }
      }

      @Test
      fun `should return bad request if prison invalid`() {
        webTestClient.upsertCourtScheduleOut(request = aRequest(prison = "UNKNOWN"))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN")
          }
      }

      @Test
      fun `should return bad request if court invalid`() {
        webTestClient.upsertCourtScheduleOut(request = aRequest(court = "UNKNOWN"))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN")
          }
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
        webTestClient.put()
          .uri("/movements/$offenderNo/court/schedule/out")
          .bodyValue(aRequest())
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/court/schedule/out")
          .headers(setAuthorisation())
          .bodyValue(aRequest())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/court/schedule/out")
          .headers(setAuthorisation("ROLE_INVALID"))
          .bodyValue(aRequest())
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.upsertCourtScheduleOutOk(
      request: UpsertCourtScheduleOut = aRequest(),
    ) = upsertCourtScheduleOut(request)
      .isOk
      .expectBodyResponse<UpsertCourtScheduleOutResponse>()

    private fun WebTestClient.upsertCourtScheduleOut(
      request: UpsertCourtScheduleOut = aRequest(),
      offenderNo: String = offender.nomsId,
    ) = put()
      .uri("/movements/$offenderNo/court/schedule/out")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()

    private fun aRequest(
      eventId: Long? = null,
      eventStatus: String = "SCH",
      returnStatus: String? = null,
      eventType: String = "CRT",
      prison: String = "BXI",
      court: String = "LEEDYC",
    ) = UpsertCourtScheduleOut(
      eventId = eventId,
      startTime = today,
      eventType = eventType,
      eventStatus = eventStatus,
      returnStatus = returnStatus,
      comment = "court schedule out comment",
      prison = prison,
      court = court,
    )
  }

  @Nested
  @DisplayName("DELETE /movements/{offenderNo}/taps/schedule/out/{eventId}")
  inner class DeleteTapScheduleOut {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(nomsId = offenderNo) {
          booking = booking {
            scheduleOut = courtEventOut(eventStatusCode = "SCH")
          }
        }
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `should delete schedule`() {
        webTestClient.deleteCourtScheduleOut()
          .expectStatus().isNoContent

        repository.runInTransaction {
          assertThat(courtEventRepository.findByIdOrNull(scheduleOut.id)).isNull()
        }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return 204 if unknown application id sent`() {
        webTestClient.deleteCourtScheduleOut(eventId = 9999)
          .expectStatus().isNoContent
      }

      @Test
      fun `should return conflict for unknown offender`() {
        webTestClient.deleteCourtScheduleOut(offenderNo = "UNKNOWN")
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 for wrong offender`() {
        nomisDataBuilder.build {
          offender(nomsId = "A7897WW")
        }

        webTestClient.deleteCourtScheduleOut(offenderNo = "A7897WW")
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if scheduled has a movement`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut(eventStatusCode = "SCH") {
                courtMovementOut()
              }
            }
          }
        }

        webTestClient.deleteCourtScheduleOut()
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if status is completed`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut(eventStatusCode = "COMP")
            }
          }
        }

        webTestClient.deleteCourtScheduleOut()
          .expectStatus().isEqualTo(409)
      }

      @Test
      fun `should return 409 if there is an inbound schedule`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut {
                courtEventIn()
              }
            }
          }
        }

        webTestClient.deleteCourtScheduleOut()
          .expectStatus().isEqualTo(409)
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorized for missing token`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/court/schedule/out/${scheduleOut.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/court/schedule/out/${scheduleOut.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.delete()
          .uri("/movements/$offenderNo/court/schedule/out/${scheduleOut.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.deleteCourtScheduleOut(
      offenderNo: String = offender.nomsId,
      eventId: Long = scheduleOut.id,
    ): WebTestClient.ResponseSpec = delete()
      .uri("/movements/$offenderNo/court/schedule/out/$eventId")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()
  }
}
