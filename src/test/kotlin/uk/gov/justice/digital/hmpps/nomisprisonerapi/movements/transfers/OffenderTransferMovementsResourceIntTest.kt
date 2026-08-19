package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferMovementOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleWaitList
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers.offender.OffenderTransferMovementsResponse

class OffenderTransferMovementsResourceIntTest(
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var schedule: OffenderTransferScheduleOut
  private lateinit var waitlist: OffenderTransferScheduleWaitList
  private lateinit var staff: Staff
  private lateinit var movement: OffenderTransferMovementOut
  private lateinit var unscheduledMovement: OffenderTransferMovementOut

  @AfterEach
  fun tearDown() {
    repository.deleteOffenders()
  }

  @Nested
  inner class GetOffenderTransferMovements {

    @Nested
    inner class HappyPath {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          staff = staff()
          offender = offender(nomsId = "A1234BC") {
            booking = booking {
              schedule = transferScheduleOut(
                comment = "schedule comment",
                hiddenComment = "schedule hidden comment",
                escort = "PECS",
                cancellationReasonCode = "TRANS",
              ) {
                waitlist = waitList(
                  approvedStaff = staff,
                  cancellationReasonCode = "ADMI",
                  commentText1 = "waitlist comment 1",
                  commentText2 = "waitlist comment 2",
                )
                movement = transferMovementOut()
              }
              unscheduledMovement = transferMovementOut()
            }
          }
        }
      }

      @Test
      fun `should return booking details`() {
        webTestClient.getOffenderTransferMovementsOk()
          .apply {
            assertThat(bookings[0].bookingId).isEqualTo(booking.bookingId)
            assertThat(bookings[0].activeBooking).isTrue
            assertThat(bookings[0].latestBooking).isTrue
          }
      }

      @Test
      fun `should return schedule details`() {
        webTestClient.getOffenderTransferMovementsOk()
          .apply {
            with(bookings[0].transferSchedules[0].schedule) {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(eventId).isEqualTo(schedule.eventId)
              assertThat(startTime).isEqualTo(schedule.getAppointmentStartDateAndTime())
              assertThat(eventSubType).isEqualTo(schedule.eventSubType.code)
              assertThat(eventStatus).isEqualTo(schedule.eventStatus.code)
              assertThat(comment).isEqualTo(schedule.comment)
              assertThat(hiddenComment).isEqualTo(schedule.hiddenComment)
              assertThat(fromPrison).isEqualTo(schedule.fromAgency?.id)
              assertThat(toPrison).isEqualTo(schedule.toAgency?.id)
              assertThat(cancellationReasonCode).isEqualTo(schedule.cancellationReasonCode?.code)
              assertThat(escortCode).isEqualTo(schedule.escort?.code)
              assertThat(userActiveCaseloadId).isNull()
              assertThat(audit.createDatetime).isEqualTo(schedule.createDatetime)
              assertThat(audit.createUsername).isEqualTo(schedule.createUsername)
            }
          }
      }

      @Test
      fun `should return waitlist details`() {
        webTestClient.getOffenderTransferMovementsOk()
          .apply {
            with(bookings[0].transferSchedules[0].schedule.waitlist!!) {
              assertThat(requestDate).isEqualTo(waitlist.requestDate)
              assertThat(status).isEqualTo(waitlist.waitListStatus.code)
              assertThat(statusDate).isEqualTo(waitlist.statusDate)
              assertThat(priority).isEqualTo(waitlist.transferPriority?.code)
              assertThat(approved).isEqualTo(waitlist.approvedFlag)
              assertThat(approvedUserName).contains(staff.firstName).contains(staff.lastName)
              assertThat(cancellationReasonCode).isEqualTo(waitlist.cancellationReasonCode?.code)
              assertThat(comment).isEqualTo(waitlist.commentText1)
              assertThat(userActiveCaseloadId).isNull()
              assertThat(audit.createDatetime).isEqualTo(waitlist.createDatetime)
              assertThat(audit.createUsername).isEqualTo(waitlist.createUsername)
            }
          }
      }

      @Test
      fun `should return movement details`() {
        webTestClient.getOffenderTransferMovementsOk()
          .apply {
            with(bookings[0].transferSchedules[0].movement!!) {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(movement.id.sequence)
              assertThat(eventId).isEqualTo(schedule.eventId)
              assertThat(transferScheduleOutId).isEqualTo(schedule.eventId)
              assertThat(movementTime).isEqualTo(movement.getMovementDateAndTime())
              assertThat(movementReason).isEqualTo(movement.movementReason.id.reasonCode)
              assertThat(escort).isEqualTo(movement.escort?.code)
              assertThat(fromPrison).isEqualTo(movement.fromAgency?.id)
              assertThat(toPrison).isEqualTo(movement.toAgency?.id)
              assertThat(active).isEqualTo(movement.active)
              assertThat(audit.createDatetime).isEqualTo(movement.createDatetime)
              assertThat(audit.createUsername).isEqualTo(movement.createUsername)
              assertThat(userActiveCaseloadId).isNull()
            }
          }
      }

      @Test
      fun `should return unscheduled movement details`() {
        webTestClient.getOffenderTransferMovementsOk()
          .apply {
            with(bookings[0].unscheduledTransferMovements[0]) {
              assertThat(bookingId).isEqualTo(booking.bookingId)
              assertThat(sequence).isEqualTo(unscheduledMovement.id.sequence)
              assertThat(eventId).isNull()
              assertThat(transferScheduleOutId).isNull()
              assertThat(movementTime).isEqualTo(unscheduledMovement.getMovementDateAndTime())
              assertThat(movementReason).isEqualTo(unscheduledMovement.movementReason.id.reasonCode)
              assertThat(escort).isEqualTo(unscheduledMovement.escort?.code)
              assertThat(fromPrison).isEqualTo(unscheduledMovement.fromAgency?.id)
              assertThat(toPrison).isEqualTo(unscheduledMovement.toAgency?.id)
              assertThat(active).isEqualTo(unscheduledMovement.active)
              assertThat(audit.createDatetime).isEqualTo(unscheduledMovement.createDatetime)
              assertThat(audit.createUsername).isEqualTo(unscheduledMovement.createUsername)
              assertThat(userActiveCaseloadId).isNull()
            }
          }
      }
    }

    @Nested
    inner class MultipleEntities {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "A1234BC") {
            booking {
              transferScheduleOut()
              transferScheduleOut()
              transferScheduleOut {
                transferMovementOut()
              }
              transferScheduleOut {
                transferMovementOut()
              }
              transferMovementOut()
              transferMovementOut()
            }
            booking = booking {
              schedule = transferScheduleOut {
                transferMovementOut()
              }
              transferMovementOut()
            }
          }
        }
      }

      @Test
      fun `should load all entities`() {
        webTestClient.getOffenderTransferMovementsOk()
          .apply {
            assertThat(bookings.size).isEqualTo(2)
            assertThat(bookings[0].transferSchedules.size).isEqualTo(4)
            assertThat(bookings[0].transferSchedules[0].movement).isNull()
            assertThat(bookings[0].transferSchedules[1].movement).isNull()
            assertThat(bookings[0].transferSchedules[2].movement).isNotNull
            assertThat(bookings[0].transferSchedules[3].movement).isNotNull
            assertThat(bookings[0].unscheduledTransferMovements.size).isEqualTo(2)
            assertThat(bookings[1].transferSchedules.size).isEqualTo(1)
            assertThat(bookings[1].transferSchedules[0].movement).isNotNull
            assertThat(bookings[1].unscheduledTransferMovements.size).isEqualTo(1)
          }
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.getOffenderTransferMovements(offenderNo = "UNKNOWN")
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class BadData {

      @Test
      fun `should return MERGE created schedules`() {
        lateinit var mergeSchedule: OffenderTransferScheduleOut
        nomisDataBuilder.build {
          offender = offender(nomsId = "A1234BC") {
            booking = booking {
              schedule = transferScheduleOut()
              mergeSchedule = transferScheduleOut()
            }
          }
        }

        // Set the audit columns to be created by a merge
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update OFFENDER_IND_SCHEDULES
              set CREATE_USER_ID = 'SYS', AUDIT_MODULE_NAME = 'MERGE'
              where EVENT_ID = ${mergeSchedule.eventId}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderTransferMovementsOk(offenderNo = offender.nomsId)
          .apply {
            bookings[0].transferSchedules.filter { it.schedule.audit.auditModuleName == "MERGE" }.apply {
              assertThat(size).isEqualTo(1)
            }
          }
      }

      @Test
      fun `should NOT return MERGE created movements`() {
        lateinit var mergeMovement: OffenderTransferMovementOut
        lateinit var mergeUnscheduledMovement: OffenderTransferMovementOut

        nomisDataBuilder.build {
          offender = offender(nomsId = "A1234BC") {
            booking = booking {
              transferScheduleOut {
                movement = transferMovementOut()
              }
              transferScheduleOut {
                mergeMovement = transferMovementOut()
              }
              unscheduledMovement = transferMovementOut()
              mergeUnscheduledMovement = transferMovementOut()
            }
          }
        }

        // Set the audit columns to be created by a merge
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set CREATE_USER_ID = 'SYS', AUDIT_MODULE_NAME = 'MERGE'
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_SEQ in (${mergeMovement.id.sequence}, ${mergeUnscheduledMovement.id.sequence})
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderTransferMovementsOk(offenderNo = offender.nomsId)
          .apply {
            bookings[0].transferSchedules.filter { it.movement?.audit?.auditModuleName == "MERGE" }.apply {
              assertThat(size).isEqualTo(0)
            }
            bookings[0].unscheduledTransferMovements.filter { it.audit.auditModuleName == "MERGE" }.apply {
              assertThat(size).isEqualTo(0)
            }
          }
      }

      @Test
      fun `should treat orphaned movements as unscheduled`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = "A1234BC") {
            booking {
              schedule = transferScheduleOut {
                movement = transferMovementOut()
              }
            }
          }
        }

        // Delete the schedule linked to the movement
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              delete from OFFENDER_IND_SCHEDULES where EVENT_ID = ${schedule.eventId}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderTransferMovementsOk(offenderNo = offender.nomsId)
          .apply {
            assertThat(bookings[0].transferSchedules).isEmpty()
            assertThat(bookings[0].unscheduledTransferMovements.size).isEqualTo(1)
          }
      }
    }

    @Nested
    inner class QueryPerformance {
      @Test
      fun `should not run additional queries for multiple instances (no N+1 queries)`() {
        buildOffender(instancesCount = 1)
        val preparedStatementsForOneInstance = getOffenderTransfersPreparedStatements()
        repository.deleteOffenders()

        buildOffender(instancesCount = 10)
        val preparedStatementsForTenInstances = getOffenderTransfersPreparedStatements()

        assertThat(preparedStatementsForTenInstances - preparedStatementsForOneInstance)
          .withFailMessage(
            "Expected the query count to stay roughly constant as the number of instances grows " +
              "(1 instance took $preparedStatementsForOneInstance queries, 10 instances took " +
              "$preparedStatementsForTenInstances queries) - this looks like an N+1 query problem.",
          )
          .isLessThanOrEqualTo(2)
      }

      private fun buildOffender(instancesCount: Int) {
        val allStaff = mutableListOf<Staff>()
        nomisDataBuilder.build {
          // We need unique staff per waitlist because it has a @NotFound causing an N+1
          repeat(instancesCount) { index -> allStaff += staff(firstName = "Bob$index", lastName = "Smith$index") }
          offender = offender(nomsId = "A1234BC") {
            booking = booking {
              repeat(instancesCount) { index ->
                transferScheduleOut {
                  waitList(approvedStaff = allStaff[index])
                  transferMovementOut()
                }
              }
              repeat(instancesCount) {
                transferMovementOut()
              }
            }
          }
        }
      }

      private fun getOffenderTransfersPreparedStatements(): Long {
        val statistics = entityManager.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        webTestClient.getOffenderTransferMovementsOk(offender.nomsId)

        return statistics.prepareStatementCount
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/A1234BC/transfer")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/transfer")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/transfer")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  private fun WebTestClient.getOffenderTransferMovements(offenderNo: String = offender.nomsId) = get()
    .uri("/movements/$offenderNo/transfer")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getOffenderTransferMovementsOk(offenderNo: String = offender.nomsId) = getOffenderTransferMovements(offenderNo)
    .expectStatus().isOk
    .expectBodyResponse<OffenderTransferMovementsResponse>()
}
