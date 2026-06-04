package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
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

class OffenderCourtMovementsResourceIntTest(
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private val offenderNo = "C7463CC"
  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var scheduleOut: CourtEvent
  private lateinit var movementOut: OffenderCourtMovementOut
  private lateinit var unscheduledMergeMovementOut: OffenderCourtMovementOut
  private lateinit var movementIn: OffenderCourtMovementIn
  private lateinit var staff: Staff
  private lateinit var courtCase: CourtCase
  private lateinit var mergeBooking: OffenderBooking
  private lateinit var mergeMovementIn: OffenderCourtMovementIn
  private lateinit var unscheduledMergeMovementIn: OffenderCourtMovementIn
  private lateinit var scheduleIn: CourtEvent

  @AfterEach
  fun tearDown() {
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
              scheduleOut = courtEventOut()
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
              scheduleOut = courtEventOut(whenCreated = LocalDateTime.now().minusHours(6))
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
              scheduleOut = courtEventOut {
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
              scheduleOut = courtEventOut {
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
    inner class Validation {
      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.getOffenderCourtMovements(offenderNo = "UNKNOWN")
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class BadData {

      @Test
      fun `should not return any movements created during merges`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            mergeBooking = booking {
              courtEventOut()
              mergeMovementIn = courtMovementIn()
              // Note there are no schedule movements OUT created by merge as this would break a NOMIS constraint
              unscheduledMergeMovementOut = courtMovementOut()
              unscheduledMergeMovementIn = courtMovementIn()
            }
            booking = booking {
              scheduleOut = courtEventOut {
                movementOut = courtMovementOut()
                movementIn = courtMovementIn()
              }
            }
          }
        }

        // Set the audit columns on the movements created by a merge to indicate a merge was responsible
        repository.runInTransaction {
          // An extra movement IN that is linked to the wrong schedule
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set CREATE_USER_ID = 'SYS', AUDIT_MODULE_NAME = 'MERGE', PARENT_EVENT_ID = ${scheduleOut.id}
              where OFFENDER_BOOK_ID = ${mergeBooking.bookingId} and MOVEMENT_SEQ = ${mergeMovementIn.id.sequence}
            """.trimIndent(),
          ).executeUpdate()

          // An extra movement IN that is unscheduled
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set CREATE_USER_ID = 'SYS', AUDIT_MODULE_NAME = 'MERGE'
              where OFFENDER_BOOK_ID = ${mergeBooking.bookingId} and MOVEMENT_SEQ = ${unscheduledMergeMovementIn.id.sequence}
            """.trimIndent(),
          ).executeUpdate()

          // An extra movement OUT that is unscheduled
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set CREATE_USER_ID = 'SYS', AUDIT_MODULE_NAME = 'MERGE'
              where OFFENDER_BOOK_ID = ${mergeBooking.bookingId} and MOVEMENT_SEQ = ${unscheduledMergeMovementOut.id.sequence}
            """.trimIndent(),
          ).executeUpdate()
        }

        // The movements created by a merge should not be returned
        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            // Check that the movements IN created by the merge isn't returned at all
            val allMovementsIn = bookings.flatMap { it.courtSchedules }.mapNotNull { it.courtMovementIn } +
              bookings.flatMap { it.unscheduledCourtMovementIns }
            assertThat(allMovementsIn.filter { it.audit.createUsername == "SYS" && it.audit.auditModuleName == "MERGE" }.size).isEqualTo(0)

            // Check that the movements OUT created by the merge isn't returned at all
            val allMovementsOut = bookings.flatMap { it.courtSchedules }.mapNotNull { it.courtMovementOut } +
              bookings.flatMap { it.unscheduledCourtMovementOuts }
            assertThat(allMovementsOut.filter { it.audit.createUsername == "SYS" && it.audit.auditModuleName == "MERGE" }.size).isEqualTo(0)
          }
      }

      @Test
      fun `should ignore orphaned schedule IN`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut {
                scheduleIn = courtEventIn()
              }
              movementOut = courtMovementOut()
              movementIn = courtMovementIn()
            }
          }
        }

        // Link the schedule IN to the schedule OUT, but then delete the schedule OUT
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update COURT_EVENTS
              set PARENT_EVENT_ID = ${scheduleOut.id}
              where EVENT_ID = ${scheduleIn.id}
            """.trimIndent(),
          ).executeUpdate()
          entityManager.createNativeQuery(
            """
              delete from COURT_EVENTS
              where EVENT_ID = ${scheduleOut.id}
            """.trimIndent(),
          ).executeUpdate()
        }

        // The orphaned schedule IN doesn't break anything
        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0]) {
              assertThat(courtSchedules).isEmpty()
              assertThat(unscheduledCourtMovementOuts).hasSize(1)
              assertThat(unscheduledCourtMovementIns).hasSize(1)
            }
          }
      }

      @Test
      fun `should treat orphaned movements as unscheduled`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut {
                movementOut = courtMovementOut()
                movementIn = courtMovementIn()
              }
            }
          }
        }

        // Remove the schedule OUT attached to the movements
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              delete from COURT_EVENTS
              where EVENT_ID = ${scheduleOut.id}
            """.trimIndent(),
          ).executeUpdate()
        }

        // The movements are returned as unscheduled
        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0]) {
              assertThat(unscheduledCourtMovementOuts).hasSize(1)
              assertThat(unscheduledCourtMovementIns).hasSize(1)
            }
          }
      }

      @Test
      fun `should not return any schedules with null direction code`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut {
                movementOut = courtMovementOut()
              }
            }
          }
        }

        // Set the schedule direction code to null
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update COURT_EVENTS
              set DIRECTION_CODE = null
              where EVENT_ID = ${scheduleOut.id}
            """.trimIndent(),
          ).executeUpdate()
        }

        // The court schedule without direction code should not be returned
        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0]) {
              assertThat(courtSchedules).isEmpty()
              assertThat(unscheduledCourtMovementOuts).hasSize(1)
            }
          }
      }

      @Test
      fun `should return incorrect data where prison or court transposed`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut(agencyId = "LEI") {
                movementOut = courtMovementOut(fromPrison = "LEEDYC", toCourt = "LEI")
                movementIn = courtMovementIn(fromCourt = "LEI", toPrison = "LEEDYC")
              }
            }
          }
        }

        // The court schedule without direction code should not be returned
        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0].courtSchedules[0]) {
              assertThat(this.court).isEqualTo("LEI")
              assertThat(courtMovementOut?.fromPrison).isEqualTo("LEEDYC")
              assertThat(courtMovementOut?.toCourt).isEqualTo("LEI")
              assertThat(courtMovementIn?.fromCourt).isEqualTo("LEI")
              assertThat(courtMovementIn?.toPrison).isEqualTo("LEEDYC")
            }
          }
      }

      @Test
      fun `should return null court if it is missing`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            booking = booking {
              scheduleOut = courtEventOut {
                movementOut = courtMovementOut()
                movementIn = courtMovementIn()
              }
            }
          }
        }

        // Set the movement courts to null
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set TO_AGY_LOC_ID = null
              where OFFENDER_BOOK_ID = ${movementOut.id.offenderBooking.bookingId} and MOVEMENT_SEQ = ${movementOut.id.sequence}
            """.trimIndent(),
          ).executeUpdate()
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set FROM_AGY_LOC_ID = null
              where OFFENDER_BOOK_ID = ${movementIn.id.offenderBooking.bookingId} and MOVEMENT_SEQ = ${movementIn.id.sequence}
            """.trimIndent(),
          ).executeUpdate()
        }

        // The null courts are returned
        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            with(bookings[0].courtSchedules[0]) {
              assertThat(courtMovementOut?.toCourt).isNull()
              assertThat(courtMovementIn?.fromCourt).isNull()
            }
          }
      }

      @Test
      fun `should not include duplicate movements created by merge on wrong booking`() {
        nomisDataBuilder.build {
          offender = offender(nomsId = offenderNo) {
            mergeBooking = booking {
              courtEventOut()
              courtMovementOut()
              mergeMovementIn = courtMovementIn()
            }
            booking = booking {
              scheduleOut = courtEventOut {
                movementOut = courtMovementOut()
                movementIn = courtMovementIn()
              }
            }
          }
        }

        repository.runInTransaction {
          // Emulate a movement IN created by merge on the wrong booking but with audit module name changed
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set CREATE_USER_ID = 'SYS', AUDIT_MODULE_NAME = 'CHANGED_FROM_MERGE', PARENT_EVENT_ID = ${scheduleOut.id}
              where OFFENDER_BOOK_ID = ${mergeBooking.bookingId} and MOVEMENT_SEQ = ${mergeMovementIn.id.sequence}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderCourtMovementsOk(offenderNo)
          .apply {
            // The movement IN on the wrong booking is not returned
            with(bookings.find { it.bookingId == booking.bookingId }!!.courtSchedules[0]) {
              assertThat(courtMovementIn?.audit?.createUsername).isNotEqualTo("SYS")
            }
            // The duplicate movement IN is not included as unscheduled either
            with(bookings.flatMap { it.unscheduledCourtMovementIns }.map { it.audit.createUsername }) {
              assertThat(this).doesNotContain("SYS")
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
