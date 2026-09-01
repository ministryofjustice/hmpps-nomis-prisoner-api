package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule.TransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule.UpsertTransferScheduleOut
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class TransferScheduleResourceIntTest(
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private val offenderNo = "B7463BB"
  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var scheduleOut: OffenderTransferScheduleOut
  private lateinit var staff: Staff

  @AfterEach
  fun tearDown() {
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /movements/{offenderNo}/transfers/schedule/out/{eventId}")
  inner class GetTransferScheduleOut {

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
              scheduleOut = transferScheduleOut(
                eventDate = LocalDate.now(),
                startTime = LocalDate.now().atTime(10, 0),
                eventSubType = "NOTR",
                eventStatus = "SCH",
                fromPrison = "BXI",
                toPrison = "LEI",
                comment = "Some comment",
                hiddenComment = "Some hidden comment",
                escort = "U",
              ) {
                waitList(
                  requestDate = LocalDate.now(),
                  waitListStatus = "PEN",
                  statusDate = LocalDate.now(),
                  transferPriority = "1",
                  approvedFlag = true,
                  approvedStaff = staff,
                  cancellationReasonCode = "ADMI",
                  commentText1 = "comment 1",
                  commentText2 = "comment 2",
                )
              }
            }
          }
        }
      }

      @Test
      fun `should get all transfer schedule details`() {
        webTestClient.getTransferScheduleOutOk(offenderNo, scheduleOut.eventId)
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(eventId).isEqualTo(scheduleOut.eventId)
            assertThat(startTime).isEqualTo(scheduleOut.getAppointmentStartDateAndTime())
            assertThat(eventSubType).isEqualTo(scheduleOut.eventSubType.code)
            assertThat(eventStatus).isEqualTo(scheduleOut.eventStatus.code)
            assertThat(comment).isEqualTo(scheduleOut.comment)
            assertThat(hiddenComment).isEqualTo(scheduleOut.hiddenComment)
            assertThat(fromPrison).isEqualTo(scheduleOut.fromAgency?.id)
            assertThat(toPrison).isEqualTo(scheduleOut.toAgency?.id)
            assertThat(escortCode).isEqualTo(scheduleOut.escort?.code)
            assertThat(cancellationReasonCode).isNull()
            assertThat(audit.createUsername).isEqualTo("SA")
            assertThat(audit.createDatetime).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
            assertThat(userActiveCaseloadId).isEqualTo("CADM_I")

            assertThat(waitlist).isNotNull
            with(waitlist!!) {
              assertThat(requestDate).isEqualTo(LocalDate.now())
              assertThat(status).isEqualTo("PEN")
              assertThat(statusDate).isEqualTo(LocalDate.now())
              assertThat(priority).isEqualTo("1")
              assertThat(approved).isTrue()
              assertThat(approvedUserName).isEqualTo(staff.accounts.first().username)
              assertThat(cancellationReasonCode).isEqualTo("ADMI")
              assertThat(comment).isEqualTo("comment 1")
              assertThat(userActiveCaseloadId).isEqualTo("CADM_I")
            }
          }
      }

      @Test
      fun `should allow missing waitlist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "LEI")
            }
          }
        }

        webTestClient.getTransferScheduleOutOk(offenderNo, scheduleOut.eventId)
          .apply {
            assertThat(waitlist).isNull()
          }
      }

      @Test
      fun `should default priority when the underlying code is invalid`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "LEI") {
                waitList(transferPriority = "1")
              }
            }
          }
        }

        repository.runInTransaction {
          // Corrupt the transfer priority code, as can happen in production
          entityManager.createNativeQuery(
            """
              update OFFENDER_IND_SCH_WAIT_LISTS set TRANSFER_PRIORITY = 'INVALID' where EVENT_ID = ${scheduleOut.eventId}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getTransferScheduleOutOk(offenderNo, scheduleOut.eventId)
          .apply {
            assertThat(waitlist!!.priority).isEqualTo("2")
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.getTransferScheduleOut(offenderNo = "UNKNOWN", eventId = 1)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found if transfer schedule doesn't exist`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "LEI")
            }
          }
        }

        webTestClient.getTransferScheduleOut(offenderNo = offenderNo, eventId = 9999)
          .expectStatus().isNotFound
      }

      @Test
      fun `should return not found if transfer schedule belongs to a different offender`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = transferScheduleOut(fromPrison = "BXI", toPrison = "LEI")
            }
          }
        }

        webTestClient.getTransferScheduleOut(offenderNo = "A9999BB", eventId = scheduleOut.eventId)
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/transfers/schedule/out/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/transfers/schedule/out/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/transfers/schedule/out/1")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    private fun WebTestClient.getTransferScheduleOut(offenderNo: String, eventId: Long) = get()
      .uri("/movements/$offenderNo/transfers/schedule/out/$eventId")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .exchange()

    private fun WebTestClient.getTransferScheduleOutOk(offenderNo: String, eventId: Long) = getTransferScheduleOut(offenderNo, eventId)
      .expectStatus().isOk
      .expectBodyResponse<TransferScheduleOut>()
  }

  @Nested
  @DisplayName("PUT /movements/{offenderNo}/transfers/schedule/out")
  inner class PutTransferScheduleOut {

    @Nested
    inner class Security {
      private fun aRequest() = UpsertTransferScheduleOut(
        eventSubType = "ANY",
        fromPrison = "ANY",
        eventStatus = "ANY",
      )

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/transfers/schedule/out")
          .bodyValue(aRequest())
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/transfers/schedule/out")
          .headers(setAuthorisation())
          .bodyValue(aRequest())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.put()
          .uri("/movements/$offenderNo/transfers/schedule/out")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .bodyValue(aRequest())
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
