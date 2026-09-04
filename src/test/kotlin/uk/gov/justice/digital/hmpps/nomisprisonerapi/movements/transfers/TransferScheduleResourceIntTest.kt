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
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderTransferScheduleOutRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.MovementHelpers.Companion.MAX_TRANSFER_SCHEDULER_COMMENT_LENGTH
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule.TransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule.UpsertTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule.UpsertTransferScheduleOutResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.schedule.UpsertTransferScheduleWaitlist
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

class TransferScheduleResourceIntTest(
  @Autowired private val entityManager: EntityManager,
  @Autowired private val transferScheduleRepository: OffenderTransferScheduleOutRepository,
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
    private val scheduleStartTime = LocalDate.now().atTime(10, 0)
    lateinit var staff: Staff

    private fun aRequest(eventId: Long? = null) = UpsertTransferScheduleOut(
      eventId = eventId,
      startTime = scheduleStartTime,
      eventSubType = "NOTR",
      eventStatus = "SCH",
      fromPrison = "BXI",
      toPrison = "LEI",
      comment = "Some comment",
      escortCode = "U",
      waitlist = UpsertTransferScheduleWaitlist(
        requestDate = LocalDate.now().minusDays(1),
        status = "CON",
        priority = "1",
        comment = "comment 1",
        approvedUserName = "MCBOBBY_GEN",
      ),
    )

    @Nested
    inner class Create {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          staff = staff("Bobby", "McBobby") {
            account(username = "MCBOBBY_GEN")
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking()
          }
        }
      }

      @Test
      fun `should create transfer schedule and waitlist`() {
        webTestClient.upsertTransferScheduleOutOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!) {
                assertThat(getAppointmentStartDateAndTime()).isEqualTo(scheduleStartTime)
                assertThat(eventSubType.code).isEqualTo("NOTR")
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(fromAgency?.id).isEqualTo("BXI")
                assertThat(toAgency?.id).isEqualTo("LEI")
                assertThat(comment).isEqualTo("Some comment")
                assertThat(escort?.code).isEqualTo("U")
                with(waitList!!) {
                  assertThat(requestDate).isEqualTo(LocalDate.now().minusDays(1))
                  assertThat(waitListStatus.code).isEqualTo("CON")
                  assertThat(statusDate).isEqualTo(LocalDate.now())
                  assertThat(transferPriority?.code).isEqualTo("1")
                  assertThat(approvedFlag).isTrue
                  assertThat(cancellationReasonCode).isNull()
                  assertThat(commentText1).isEqualTo("comment 1")
                  assertThat(approvedStaff?.id).isEqualTo(staff.id)
                }
              }
            }
          }
      }

      @Test
      fun `should not create schedule hidden comment or cancellation reason`() {
        webTestClient.upsertTransferScheduleOutOk()
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!) {
                assertThat(hiddenComment).isNull()
                assertThat(cancellationReasonCode).isNull()
              }
            }
          }
      }

      @Test
      fun `should create transfer schedule with null waitlist`() {
        webTestClient.upsertTransferScheduleOutOk(request = aRequest().copy(waitlist = null))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!) {
                assertThat(waitList).isNull()
              }
            }
          }
      }

      @Test
      fun `should create comments that fit in NOMIS`() {
        webTestClient.upsertTransferScheduleOutOk(
          request = aRequest().let {
            it.copy(
              comment = "1234567890".repeat(30),
              waitlist = it.waitlist!!.copy(comment = "1234567890".repeat(30)),
            )
          },
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!) {
                assertThat(comment?.length).isEqualTo(MAX_TRANSFER_SCHEDULER_COMMENT_LENGTH)
                assertThat(waitList?.commentText1?.length).isEqualTo(MAX_TRANSFER_SCHEDULER_COMMENT_LENGTH)
              }
            }
          }
      }

      @Test
      fun `should create transfer schedule with null approved staff if not found`() {
        webTestClient.upsertTransferScheduleOutOk(
          request = aRequest().let { it.copy(waitlist = it.waitlist!!.copy(approvedUserName = "UNKNOWN")) },
        )
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!) {
                assertThat(waitList!!.approvedStaff).isNull()
              }
            }
          }
      }
    }

    @Nested
    inner class Update {
      lateinit var scheduleNoWaitlist: OffenderTransferScheduleOut

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          staff = staff("Bobby", "McBobby") {
            account(username = "MCBOBBY_GEN")
          }
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = transferScheduleOut(
                hiddenComment = "Should not change",
                cancellationReasonCode = "TRANS",
              ) {
                waitList()
              }
              scheduleNoWaitlist = transferScheduleOut()
            }
          }
        }
      }

      @Test
      fun `should update transfer schedule and waitlist`() {
        webTestClient.upsertTransferScheduleOutOk(request = aRequest(eventId = scheduleOut.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(eventId).isEqualTo(scheduleOut.eventId)
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!) {
                assertThat(getAppointmentStartDateAndTime()).isEqualTo(scheduleStartTime)
                assertThat(eventSubType.code).isEqualTo("NOTR")
                assertThat(eventStatus.code).isEqualTo("SCH")
                assertThat(fromAgency?.id).isEqualTo("BXI")
                assertThat(toAgency?.id).isEqualTo("LEI")
                assertThat(comment).isEqualTo("Some comment")
                assertThat(escort?.code).isEqualTo("U")
                with(waitList!!) {
                  assertThat(requestDate).isEqualTo(LocalDate.now().minusDays(1))
                  assertThat(waitListStatus.code).isEqualTo("CON")
                  assertThat(statusDate).isEqualTo(LocalDate.now())
                  assertThat(transferPriority?.code).isEqualTo("1")
                  assertThat(approvedFlag).isTrue
                  assertThat(cancellationReasonCode).isNull()
                  assertThat(commentText1).isEqualTo("comment 1")
                  assertThat(approvedStaff?.id).isEqualTo(staff.id)
                }
              }
            }
          }
      }

      @Test
      fun `should update schedule hidden comment or cancellation reason`() {
        webTestClient.upsertTransferScheduleOutOk(request = aRequest(eventId = scheduleOut.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(eventId).isEqualTo(scheduleOut.eventId)
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!) {
                assertThat(hiddenComment).isEqualTo("Should not change")
                assertThat(cancellationReasonCode?.code).isEqualTo("TRANS")
              }
            }
          }
      }

      @Test
      fun `should remove waitlist`() {
        webTestClient.upsertTransferScheduleOutOk(request = aRequest(eventId = scheduleOut.eventId).copy(waitlist = null))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(eventId).isEqualTo(scheduleOut.eventId)
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!) {
                assertThat(waitList).isNull()
              }
            }
          }
      }

      @Test
      fun `should add waitlist`() {
        webTestClient.upsertTransferScheduleOutOk(request = aRequest(eventId = scheduleNoWaitlist.eventId))
          .apply {
            assertThat(bookingId).isEqualTo(booking.bookingId)
            assertThat(eventId).isEqualTo(scheduleNoWaitlist.eventId)
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!) {
                assertThat(waitList).isNotNull()
                assertThat(waitList?.id).isEqualTo(scheduleNoWaitlist.eventId)
              }
            }
          }
      }
    }

    @Nested
    inner class WaitListStatusTransitions {

      @Nested
      inner class NewScheduleAndWaitlist {
        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            staff = staff("Bobby", "McBobby") {
              account(username = "MCBOBBY_GEN")
            }
            offender = offender(nomsId = offenderNo) {
              booking = booking()
            }
          }
        }

        @Test
        fun `new pending waitlist`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest().let {
              it.copy(eventStatus = "PEN", waitlist = it.waitlist!!.copy(status = "PEN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("PEN")
                assertThat(statusDate).isEqualTo(LocalDate.now())
                assertThat(approvedFlag).isFalse
                assertThat(approvedStaff).isNull()
                assertThat(cancellationReasonCode).isNull()
              }
            }
          }
        }

        @Test
        fun `new confirmed waitlist`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest().let {
              it.copy(eventStatus = "SCH", waitlist = it.waitlist!!.copy(status = "CON", approvedUserName = "MCBOBBY_GEN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("CON")
                assertThat(statusDate).isEqualTo(LocalDate.now())
                assertThat(approvedFlag).isTrue
                assertThat(approvedStaff).isEqualTo(staff)
                assertThat(cancellationReasonCode).isNull()
              }
            }
          }
        }

        @Test
        fun `new cancelled waitlist`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest().let {
              it.copy(eventStatus = "CANC", waitlist = it.waitlist!!.copy(status = "CAN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("CAN")
                assertThat(statusDate).isEqualTo(LocalDate.now())
                assertThat(approvedFlag).isFalse
                assertThat(approvedStaff).isNull()
                assertThat(cancellationReasonCode?.code).isEqualTo("ADMI")
              }
            }
          }
        }
      }

      @Nested
      inner class ExistingPendingWaitlist {
        lateinit var pendingSchedule: OffenderTransferScheduleOut

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            staff = staff("Bobby", "McBobby") {
              account(username = "MCBOBBY_GEN")
            }
            offender = offender(nomsId = offenderNo) {
              booking = booking {
                pendingSchedule = transferScheduleOut(eventStatus = "PEN") {
                  waitList(
                    waitListStatus = "PEN",
                    statusDate = LocalDate.now().minusDays(1),
                    approvedFlag = false,
                    approvedStaff = null,
                  )
                }
              }
            }
          }
        }

        @Test
        fun `pending waitlist unchanged`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest(eventId = pendingSchedule.eventId).let {
              it.copy(eventStatus = "PEN", waitlist = it.waitlist!!.copy(status = "PEN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("PEN")
                // status date still yesterday
                assertThat(statusDate).isEqualTo(LocalDate.now().minusDays(1))
                assertThat(approvedFlag).isFalse
                assertThat(approvedStaff).isNull()
                assertThat(cancellationReasonCode).isNull()
              }
            }
          }
        }

        @Test
        fun `pending waitlist confirmed`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest(eventId = pendingSchedule.eventId).let {
              it.copy(eventStatus = "SCH", waitlist = it.waitlist!!.copy(status = "CON", approvedUserName = "MCBOBBY_GEN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("CON")
                assertThat(statusDate).isEqualTo(LocalDate.now())
                assertThat(approvedFlag).isTrue
                // approved staff is taken from request
                assertThat(approvedStaff).isEqualTo(staff)
                assertThat(cancellationReasonCode).isNull()
              }
            }
          }
        }

        @Test
        fun `pending waitlist cancelled`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest(eventId = pendingSchedule.eventId).let {
              it.copy(eventStatus = "CANC", waitlist = it.waitlist!!.copy(status = "CAN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("CAN")
                assertThat(statusDate).isEqualTo(LocalDate.now())
                assertThat(approvedFlag).isFalse
                assertThat(approvedStaff).isNull()
                // Cancellation reason code uses hardcoded value
                assertThat(cancellationReasonCode?.code).isEqualTo("ADMI")
              }
            }
          }
        }
      }

      @Nested
      inner class ExistingConfirmedWaitlist {
        lateinit var confirmedSchedule: OffenderTransferScheduleOut
        lateinit var staff2: Staff

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            staff = staff("Bobby", "McBobby") {
              account(username = "MCBOBBY_GEN")
            }
            staff2 = staff("Barry", "Barry") {
              account(username = "BARRY_GEN")
            }
            offender = offender(nomsId = offenderNo) {
              booking = booking {
                confirmedSchedule = transferScheduleOut(eventStatus = "SCH") {
                  waitList(
                    waitListStatus = "CON",
                    statusDate = LocalDate.now().minusDays(1),
                    approvedFlag = true,
                    approvedStaff = staff,
                  )
                }
              }
            }
          }
        }

        @Test
        fun `confirmed waitlist unchanged`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest(eventId = confirmedSchedule.eventId).let {
              it.copy(eventStatus = "SCH", waitlist = it.waitlist!!.copy(status = "CON", approvedUserName = "BARRY_GEN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("CON")
                // status date still yesterday
                assertThat(statusDate).isEqualTo(LocalDate.now().minusDays(1))
                assertThat(approvedFlag).isTrue
                // approved staff is not updated from request
                assertThat(approvedStaff).isEqualTo(staff)
                assertThat(cancellationReasonCode).isNull()
              }
            }
          }
        }

        @Test
        fun `confirmed waitlist changed to pending`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest(eventId = confirmedSchedule.eventId).let {
              it.copy(eventStatus = "PEN", waitlist = it.waitlist!!.copy(status = "PEN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("PEN")
                assertThat(statusDate).isEqualTo(LocalDate.now())
                assertThat(approvedFlag).isFalse
                assertThat(approvedStaff).isNull()
                assertThat(cancellationReasonCode).isNull()
              }
            }
          }
        }

        @Test
        fun `confirmed waitlist changed to cancelled`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest(eventId = confirmedSchedule.eventId).let {
              it.copy(eventStatus = "CANC", waitlist = it.waitlist!!.copy(status = "CAN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("CAN")
                assertThat(statusDate).isEqualTo(LocalDate.now())
                assertThat(approvedFlag).isFalse
                assertThat(approvedStaff).isNull()
                assertThat(cancellationReasonCode?.code).isEqualTo("ADMI")
              }
            }
          }
        }
      }

      @Nested
      inner class ExistingCancelledWaitlist {
        lateinit var cancelledSchedule: OffenderTransferScheduleOut

        @BeforeEach
        fun setUp() {
          nomisDataBuilder.build {
            staff = staff("Bobby", "McBobby") {
              account(username = "MCBOBBY_GEN")
            }
            offender = offender(nomsId = offenderNo) {
              booking = booking {
                cancelledSchedule = transferScheduleOut(eventStatus = "CANC") {
                  waitList(
                    waitListStatus = "CAN",
                    statusDate = LocalDate.now().minusDays(1),
                    approvedFlag = false,
                    approvedStaff = null,
                    cancellationReasonCode = "TRANS",
                  )
                }
              }
            }
          }
        }

        @Test
        fun `cancelled waitlist unchanged`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest(eventId = cancelledSchedule.eventId).let {
              it.copy(eventStatus = "CANC", waitlist = it.waitlist!!.copy(status = "CAN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("CAN")
                // status date still yesterday
                assertThat(statusDate).isEqualTo(LocalDate.now().minusDays(1))
                assertThat(approvedFlag).isFalse
                assertThat(approvedStaff).isNull()
                // Cancelled reason is not updated from existing value
                assertThat(cancellationReasonCode?.code).isEqualTo("TRANS")
              }
            }
          }
        }

        @Test
        fun `cancelled waitlist changed to pending`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest(eventId = cancelledSchedule.eventId).let {
              it.copy(eventStatus = "PEN", waitlist = it.waitlist!!.copy(status = "PEN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("PEN")
                assertThat(statusDate).isEqualTo(LocalDate.now())
                assertThat(approvedFlag).isFalse
                assertThat(approvedStaff).isNull()
                assertThat(cancellationReasonCode).isNull()
              }
            }
          }
        }

        @Test
        fun `cancelled waitlist changed to confirmed`() {
          webTestClient.upsertTransferScheduleOutOk(
            request = aRequest(eventId = cancelledSchedule.eventId).let {
              it.copy(eventStatus = "SCH", waitlist = it.waitlist!!.copy(status = "CON", approvedUserName = "MCBOBBY_GEN"))
            },
          ).apply {
            repository.runInTransaction {
              with(transferScheduleRepository.findByIdOrNull(eventId)!!.waitList!!) {
                assertThat(waitListStatus.code).isEqualTo("CON")
                assertThat(statusDate).isEqualTo(LocalDate.now())
                assertThat(approvedFlag).isTrue
                assertThat(approvedStaff).isEqualTo(staff)
                assertThat(cancellationReasonCode).isNull()
              }
            }
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
        webTestClient.upsertTransferScheduleOut(offenderNo = "UNKNOWN")
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

        webTestClient.upsertTransferScheduleOut()
          .isNotFound
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("C1234DE").contains("not found")
          }
      }

      @Test
      fun `should return bad request if event sub type invalid`() {
        webTestClient.upsertTransferScheduleOut(request = aRequest().copy(eventSubType = "UNKNOWN"))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN")
          }
      }

      @Test
      fun `should return bad request if event status invalid`() {
        webTestClient.upsertTransferScheduleOut(request = aRequest().copy(eventStatus = "UNKNOWN"))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN")
          }
      }

      @Test
      fun `should return bad request if escort invalid`() {
        webTestClient.upsertTransferScheduleOut(request = aRequest().copy(escortCode = "UNKNOWN"))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN")
          }
      }

      @Test
      fun `should return bad request if from prison invalid`() {
        webTestClient.upsertTransferScheduleOut(request = aRequest().copy(fromPrison = "UNKNOWN"))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN")
          }
      }

      @Test
      fun `should return bad request if to prison invalid`() {
        webTestClient.upsertTransferScheduleOut(request = aRequest().copy(toPrison = "UNKNOWN"))
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN")
          }
      }

      @Test
      fun `should return bad request if waitlist status invalid`() {
        webTestClient.upsertTransferScheduleOut(
          request = aRequest().let {
            it.copy(waitlist = it.waitlist!!.copy(status = "UNKNOWN"))
          },
        )
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN")
          }
      }

      @Test
      fun `should return bad request if waitlist transfer priority invalid`() {
        webTestClient.upsertTransferScheduleOut(
          request = aRequest().let {
            it.copy(waitlist = it.waitlist!!.copy(priority = "UNKNOWN"))
          },
        )
          .isBadRequest
          .expectBody().jsonPath("userMessage").value<String> {
            assertThat(it).contains("UNKNOWN")
          }
      }
    }

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

    private fun WebTestClient.upsertTransferScheduleOutOk(
      request: UpsertTransferScheduleOut = aRequest(),
      recreate: Boolean = false,
    ) = upsertTransferScheduleOut(request, recreate = recreate)
      .isOk
      .expectBodyResponse<UpsertTransferScheduleOutResponse>()

    private fun WebTestClient.upsertTransferScheduleOut(
      request: UpsertTransferScheduleOut = aRequest(),
      offenderNo: String = offender.nomsId,
      recreate: Boolean = false,
    ) = put()
      .uri {
        it.path("/movements/$offenderNo/transfers/schedule/out")
          .queryParam("recreate", recreate)
          .build()
      }
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
      .bodyValue(request)
      .exchange()
      .expectStatus()
  }
}
