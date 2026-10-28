package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.SessionFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.CorporateAddressDsl.Companion.SHEFFIELD
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.roundToNearestSecond
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyLocationAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtEvent
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleIn
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTapScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.BookingPersonLocations
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.BookingStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementDirection
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementLocationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.MovementType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.OffenderPersonLocationsResponse
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class OffenderPersonLocationsResourceIntTest(
  @Autowired private val entityManager: EntityManager,
) : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private val now = LocalDateTime.now().roundToNearestSecond()
  private val yesterday = now.minusDays(1)
  private val twoDaysAgo = now.minusDays(2)

  @AfterEach
  fun `reset data`() {
    repository.deleteOffenders()
  }

  @Nested
  inner class GetOffenderPersonLocations {

    @Nested
    inner class ReceiveAndRelease {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = yesterday) {
              release(date = now)
            }
          }
        }
      }

      @Test
      fun `should get booking details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            repository.runInTransaction {
              with(bookings[0].booking) {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(bookingNumber).isEqualTo(booking.bookNumber)
                assertThat(activeBooking).isFalse
                assertThat(latestBooking).isTrue
                assertThat(bookingStatus).isEqualTo(BookingStatus.OPEN)
                assertThat(bookingBeginTime).isEqualTo(yesterday)
                assertThat(bookingEndTime).isEqualTo(now.truncatedTo(ChronoUnit.DAYS))
                assertThat(audit.createDatetime).isCloseTo(now, within(50, ChronoUnit.MINUTES))
                assertThat(audit.createUsername).isEqualTo("SA")
              }
              assertThat(bookings[0].movements).hasSize(2)
            }
          }
      }

      @Test
      fun `should get admission details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            repository.runInTransaction {
              with(bookings[0].movementWithSeq(1)) {
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
            }
          }
      }

      @Test
      fun `should get release details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            repository.runInTransaction {
              with(bookings[0].movementWithSeq(2)) {
                assertThat(movementType).isEqualTo(MovementType.REL)
                assertThat(directionCode).isEqualTo(MovementDirection.OUT)
                assertThat(movementTime).isEqualTo(now)
                assertThat(movementReason.code).isEqualTo("CR")
                assertThat(movementReason.description).isEqualTo("Conditional Release")
                assertThat(from).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
                assertThat(to).isEqualTo(MovementLocation("OUT", MovementLocationType.OTHER, "OUTSIDE"))
                assertThat(scheduleId).isNull()
                assertThat(commentText).isNull()
                assertThat(audit.createDatetime).isCloseTo(now, within(5, ChronoUnit.MINUTES))
                assertThat(audit.createUsername).isEqualTo("SA")
              }
            }
          }
      }
    }

    @Nested
    inner class TapMovements {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          agencyLocation(agencyLocationId = "NGENHO", description = "Northern General Hospital", type = "HOSPITAL")
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapMovementOut(date = yesterday, toAgency = "NGENHO", comment = "Tap out comment")
              tapMovementIn(date = now, comment = "Tap in comment")
            }
          }
        }
      }

      @Test
      fun `should get TAP out details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(2)) {
              assertThat(movementType).isEqualTo(MovementType.TAP)
              assertThat(directionCode).isEqualTo(MovementDirection.OUT)
              assertThat(movementTime).isEqualTo(yesterday)
              assertThat(movementReason.code).isEqualTo("C5")
              assertThat(from).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
              assertThat(to).isEqualTo(MovementLocation("NGENHO", MovementLocationType.HOSPITAL, "Northern General Hospital"))
              assertThat(scheduleId).isNull()
              assertThat(commentText).isEqualTo("Tap out comment")
            }
          }
      }

      @Test
      fun `should get TAP in details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(3)) {
              assertThat(movementType).isEqualTo(MovementType.TAP)
              assertThat(directionCode).isEqualTo(MovementDirection.IN)
              assertThat(movementTime).isEqualTo(now)
              assertThat(movementReason.code).isEqualTo("C5")
              assertThat(from).isEqualTo(MovementLocation("NGENHO", MovementLocationType.HOSPITAL, "Northern General Hospital"))
              assertThat(to).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
              assertThat(scheduleId).isNull()
              assertThat(commentText).isEqualTo("Tap in comment")
            }
          }
      }
    }

    @Nested
    inner class OtherLocationTypes {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          agencyLocation(agencyLocationId = "SHEFPO", description = "Sheffield Probation Office", type = "COMM")
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapMovementOut(date = yesterday, toAgency = "SHEFPO")
            }
          }
        }
      }

      @Test
      fun `should map agency locations that are not prison, court or hospital as OTHER`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movementWithSeq(2).to)
              .isEqualTo(MovementLocation("SHEFPO", MovementLocationType.OTHER, "Sheffield Probation Office"))
          }
      }
    }

    @Nested
    inner class CourtMovements {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              courtMovementOut(date = yesterday, comment = "Court out comment")
              courtMovementIn(date = now, comment = "Court in comment")
            }
          }
        }
      }

      @Test
      fun `should get court out details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(2)) {
              assertThat(movementType).isEqualTo(MovementType.CRT)
              assertThat(directionCode).isEqualTo(MovementDirection.OUT)
              assertThat(movementTime).isEqualTo(yesterday)
              assertThat(movementReason.code).isEqualTo("CRT")
              assertThat(from).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
              assertThat(to).isEqualTo(MovementLocation("ABDRCT", MovementLocationType.COURT, "court 2"))
              assertThat(scheduleId).isNull()
              assertThat(commentText).isEqualTo("Court out comment")
            }
          }
      }

      @Test
      fun `should get court in details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(3)) {
              assertThat(movementType).isEqualTo(MovementType.CRT)
              assertThat(directionCode).isEqualTo(MovementDirection.IN)
              assertThat(movementTime).isEqualTo(now)
              assertThat(movementReason.code).isEqualTo("CRT")
              assertThat(from).isEqualTo(MovementLocation("ABDRCT", MovementLocationType.COURT, "court 2"))
              assertThat(to).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
              assertThat(scheduleId).isNull()
              assertThat(commentText).isEqualTo("Court in comment")
            }
          }
      }
    }

    @Nested
    inner class TransferAfterAdmission {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              prisonTransfer(from = "BXI", to = "MDI", date = yesterday, arrivalDate = now)
            }
          }
        }
      }

      @Test
      fun `should get transfer out details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(2)) {
              assertThat(movementType).isEqualTo(MovementType.TRN)
              assertThat(directionCode).isEqualTo(MovementDirection.OUT)
              assertThat(movementTime).isEqualTo(yesterday)
              assertThat(movementReason.code).isEqualTo("28")
              assertThat(from).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
              assertThat(to).isEqualTo(MovementLocation("MDI", MovementLocationType.PRISON, "MOORLAND"))
              assertThat(scheduleId).isNull()
            }
          }
      }

      @Test
      fun `should get admission after transfer details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(3)) {
              assertThat(movementType).isEqualTo(MovementType.ADM)
              assertThat(directionCode).isEqualTo(MovementDirection.IN)
              assertThat(movementTime).isEqualTo(now)
              assertThat(movementReason.code).isEqualTo("INT")
              assertThat(from).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
              assertThat(to).isEqualTo(MovementLocation("MDI", MovementLocationType.PRISON, "MOORLAND"))
              assertThat(scheduleId).isNull()
            }
          }
      }
    }

    @Nested
    inner class TransferAfterCourt {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              courtTransfer(toPrison = "MDI", court = "ABDRCT", date = yesterday, arrivalDate = now)
            }
          }
        }
      }

      @Test
      fun `should get court out details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(2)) {
              assertThat(movementType).isEqualTo(MovementType.CRT)
              assertThat(directionCode).isEqualTo(MovementDirection.OUT)
              assertThat(from).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
              assertThat(to).isEqualTo(MovementLocation("ABDRCT", MovementLocationType.COURT, "court 2"))
            }
          }
      }

      @Test
      fun `should get admission from court details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(3)) {
              assertThat(movementType).isEqualTo(MovementType.ADM)
              assertThat(directionCode).isEqualTo(MovementDirection.IN)
              assertThat(movementTime).isEqualTo(now)
              assertThat(movementReason.code).isEqualTo("TRNCRT")
              assertThat(from).isEqualTo(MovementLocation("ABDRCT", MovementLocationType.COURT, "court 2"))
              assertThat(to).isEqualTo(MovementLocation("MDI", MovementLocationType.PRISON, "MOORLAND"))
              assertThat(scheduleId).isNull()
            }
          }
      }
    }

    @Nested
    inner class TransferAfterTap {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          agencyLocation(agencyLocationId = "NGENHO", description = "Northern General Hospital", type = "HOSPITAL")
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapTransfer(toPrison = "MDI", toAgency = "NGENHO", date = yesterday, arrivalDate = now)
            }
          }
        }
      }

      @Test
      fun `should get TAP out details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(2)) {
              assertThat(movementType).isEqualTo(MovementType.TAP)
              assertThat(directionCode).isEqualTo(MovementDirection.OUT)
              assertThat(from).isEqualTo(MovementLocation("BXI", MovementLocationType.PRISON, "BRIXTON"))
              assertThat(to).isEqualTo(MovementLocation("NGENHO", MovementLocationType.HOSPITAL, "Northern General Hospital"))
            }
          }
      }

      @Test
      fun `should get admission from TAP details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(3)) {
              assertThat(movementType).isEqualTo(MovementType.ADM)
              assertThat(directionCode).isEqualTo(MovementDirection.IN)
              assertThat(movementTime).isEqualTo(now)
              assertThat(movementReason.code).isEqualTo("TRNTAP")
              assertThat(from).isEqualTo(MovementLocation("NGENHO", MovementLocationType.HOSPITAL, "Northern General Hospital"))
              assertThat(to).isEqualTo(MovementLocation("MDI", MovementLocationType.PRISON, "MOORLAND"))
              assertThat(scheduleId).isNull()
            }
          }
      }
    }

    @Nested
    inner class ScheduledMovements {

      @Test
      fun `should get schedule ID for scheduled TAP movements`() {
        lateinit var scheduleOut: OffenderTapScheduleOut
        lateinit var scheduleIn: OffenderTapScheduleIn
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapApplication {
                scheduleOut = tapScheduleOut(fromPrison = "BXI", toAgency = "HAZLWD") {
                  tapMovementOut(date = yesterday)
                  scheduleIn = tapScheduleIn(toPrison = "BXI") {
                    tapMovementIn(date = now)
                  }
                }
              }
            }
          }
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movements).hasSize(3)
            with(bookings[0].movementWithSeq(2)) {
              assertThat(movementType).isEqualTo(MovementType.TAP)
              assertThat(directionCode).isEqualTo(MovementDirection.OUT)
              assertThat(scheduleId).isEqualTo(scheduleOut.eventId)
            }
            with(bookings[0].movementWithSeq(3)) {
              assertThat(movementType).isEqualTo(MovementType.TAP)
              assertThat(directionCode).isEqualTo(MovementDirection.IN)
              assertThat(scheduleId).isEqualTo(scheduleIn.eventId)
            }
          }
      }

      @Test
      fun `should get schedule ID for scheduled court movement OUT only`() {
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

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movements).hasSize(3)
            with(bookings[0].movementWithSeq(2)) {
              assertThat(movementType).isEqualTo(MovementType.CRT)
              assertThat(directionCode).isEqualTo(MovementDirection.OUT)
              assertThat(scheduleId).isEqualTo(scheduleOut.id)
            }
            with(bookings[0].movementWithSeq(3)) {
              assertThat(movementType).isEqualTo(MovementType.CRT)
              assertThat(directionCode).isEqualTo(MovementDirection.IN)
              assertThat(scheduleId).isNull()
            }
          }
      }

      @Test
      fun `should get schedule ID for scheduled transfer movements`() {
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

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movements).hasSize(3)
            with(bookings[0].movementWithSeq(2)) {
              assertThat(movementType).isEqualTo(MovementType.TRN)
              assertThat(scheduleId).isEqualTo(scheduleOut.eventId)
            }
            with(bookings[0].movementWithSeq(3)) {
              assertThat(movementType).isEqualTo(MovementType.ADM)
              assertThat(scheduleId).isNull()
            }
          }
      }
    }

    @Nested
    inner class TapLocations {

      private lateinit var offenderAddress: OffenderAddress
      private lateinit var agencyAddress: AgencyLocationAddress

      @Test
      fun `should use the schedule agency for a scheduled TAP`() {
        nomisDataBuilder.build {
          agencyLocation(agencyLocationId = "NGENHO", description = "Northern General Hospital", type = "HOSPITAL")
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapApplication {
                tapScheduleOut(fromPrison = "BXI", toAgency = "NGENHO") {
                  tapMovementOut(date = yesterday)
                  tapScheduleIn(toPrison = "BXI") {
                    tapMovementIn(date = now)
                  }
                }
              }
            }
          }
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movementWithSeq(2).to).isEqualTo(MovementLocation("NGENHO", MovementLocationType.HOSPITAL, "Northern General Hospital"))
            assertThat(bookings[0].movementWithSeq(3).from).isEqualTo(MovementLocation("NGENHO", MovementLocationType.HOSPITAL, "Northern General Hospital"))
          }
      }

      @Test
      fun `should use the address from the schedule if the TAP is scheduled to an address`() {
        nomisDataBuilder.build {
          offender = offender {
            offenderAddress = address(postcode = "S1 1AA")
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapApplication(toAddress = offenderAddress) {
                tapScheduleOut(fromPrison = "BXI", toAddress = offenderAddress) {
                  tapMovementOut(date = yesterday)
                  tapScheduleIn(toPrison = "BXI") {
                    tapMovementIn(date = now)
                  }
                }
              }
            }
          }
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movementWithSeq(2).to).isEqualTo(MovementLocation(null, MovementLocationType.OTHER, "41 High Street, Sheffield"))
            assertThat(bookings[0].movementWithSeq(3).from).isEqualTo(MovementLocation(null, MovementLocationType.OTHER, "41 High Street, Sheffield"))
          }
      }

      @Test
      fun `should use the address on the movement for an unscheduled TAP`() {
        nomisDataBuilder.build {
          offender = offender {
            offenderAddress = address(postcode = "S1 1AA")
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapMovement(toAddress = offenderAddress, date = yesterday, returnDate = now)
            }
          }
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movementWithSeq(2).to).isEqualTo(MovementLocation(null, MovementLocationType.OTHER, "41 High Street, Sheffield"))
            assertThat(bookings[0].movementWithSeq(3).from).isEqualTo(MovementLocation(null, MovementLocationType.OTHER, "41 High Street, Sheffield"))
          }
      }

      @Test
      fun `should use the city for an unscheduled TAP`() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapMovement(toCity = SHEFFIELD, date = yesterday, returnDate = now)
            }
          }
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movementWithSeq(2).to).isEqualTo(MovementLocation(null, MovementLocationType.OTHER, "Sheffield"))
            assertThat(bookings[0].movementWithSeq(3).from).isEqualTo(MovementLocation(null, MovementLocationType.OTHER, "Sheffield"))
          }
      }

      @Test
      fun `should prefer the agency to the address`() {
        nomisDataBuilder.build {
          agencyLocation(agencyLocationId = "NGENHO", description = "Northern General Hospital", type = "HOSPITAL") {
            agencyAddress = address(postcode = "S5 7AU")
          }
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapMovement(toAgency = "NGENHO", toAddress = agencyAddress, date = yesterday, returnDate = now)
            }
          }
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movementWithSeq(2).to).isEqualTo(MovementLocation("NGENHO", MovementLocationType.HOSPITAL, "Northern General Hospital"))
          }
      }

      @Test
      fun `should not use addresses or cities for movements that are not TAPs`() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              courtMovementOut(date = yesterday)
            }
          }
        }

        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set TO_CITY = '$SHEFFIELD', TO_AGY_LOC_ID = null
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_TYPE = 'CRT'
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movementWithSeq(2).to).isNull()
          }
      }
    }

    @Nested
    inner class MultipleBookings {

      @Test
      fun `should return movements on the correct booking`() {
        lateinit var firstBooking: OffenderBooking
        nomisDataBuilder.build {
          offender = offender {
            firstBooking = booking(bookingBeginDate = twoDaysAgo, bookingSequence = 2) {
              release(date = yesterday)
            }
            booking = booking(bookingBeginDate = now, bookingSequence = 1) {
              courtMovementOut(date = now.plusMinutes(1))
            }
          }
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings).hasSize(2)
            with(bookings.single { it.booking.bookingId == firstBooking.bookingId }) {
              assertThat(booking.latestBooking).isFalse
              assertThat(movements.map { it.movementType }).containsExactly(MovementType.ADM, MovementType.REL)
            }
            with(bookings.single { it.booking.bookingId == booking.bookingId }) {
              assertThat(booking.latestBooking).isTrue
              assertThat(movements.map { it.movementType }).containsExactly(MovementType.ADM, MovementType.CRT)
            }
          }
      }
    }

    @Nested
    inner class BookingEndDateFormats {

      @Test
      fun `should handle booking end date with and without a time`() {
        lateinit var firstBooking: OffenderBooking
        val bookingReleaseWithTime = now.plusMinutes(1)
        nomisDataBuilder.build {
          offender = offender {
            firstBooking = booking(bookingBeginDate = twoDaysAgo, bookingSequence = 2) {
              release(date = yesterday)
            }
            booking = booking(bookingBeginDate = now, bookingSequence = 1) {
              release(date = bookingReleaseWithTime)
            }
          }
        }

        repository.runInTransaction {
          // Add a time component to the second booking end time to match data found in NOMIS
          entityManager.createNativeQuery(
            """
              update OFFENDER_BOOKINGS
              set BOOKING_END_DATE = TIMESTAMP '${bookingReleaseWithTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}'
              where OFFENDER_BOOK_ID = ${booking.bookingId}
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings.single { it.booking.bookingId == firstBooking.bookingId }) {
              assertThat(booking.bookingEndTime).isEqualTo(yesterday.truncatedTo(ChronoUnit.DAYS))
            }
            with(bookings.single { it.booking.bookingId == booking.bookingId }) {
              assertThat(booking.bookingEndTime).isEqualTo(bookingReleaseWithTime)
            }
          }
      }
    }

    @Nested
    inner class BadData {

      @Test
      fun `should return null location if the movement agency is missing`() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              prisonTransfer(from = "BXI", to = "MDI", date = yesterday, arrivalDate = now)
            }
          }
        }

        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set TO_AGY_LOC_ID = null
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_TYPE = 'TRN'
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movementWithSeq(2).to).isNull()
          }
      }
    }

    @Nested
    inner class BadDataFromLocation {

      @Test
      fun `should return unknown from location for release and transfer out movements with no from location`() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              prisonTransfer(from = "BXI", to = "MDI", date = yesterday, arrivalDate = yesterday.plusMinutes(1))
              release(date = now)
            }
          }
        }

        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set FROM_AGY_LOC_ID = null
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_TYPE in ('TRN', 'REL') and DIRECTION_CODE = 'OUT'
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            with(bookings[0].movementWithSeq(2)) {
              assertThat(movementType).isEqualTo(MovementType.TRN)
              assertThat(from).isEqualTo(MovementLocation("UNKNOWN", MovementLocationType.OTHER, "Unknown"))
              assertThat(to!!.code).isEqualTo("MDI")
            }
            with(bookings[0].movementWithSeq(4)) {
              assertThat(movementType).isEqualTo(MovementType.REL)
              assertThat(from).isEqualTo(MovementLocation("UNKNOWN", MovementLocationType.OTHER, "Unknown"))
              assertThat(to).isEqualTo(MovementLocation("OUT", MovementLocationType.OTHER, "OUTSIDE"))
            }
          }
      }
    }

    @Nested
    inner class BadDataNullDirection {

      @Test
      fun `should return OK and ignore bad movement with null direction`() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              release(date = now)
            }
          }
        }

        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set DIRECTION_CODE = null
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_TYPE = 'REL'
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movements.size).isEqualTo(1)
          }
      }
    }

    @Nested
    inner class MergedMovements {

      @Test
      fun `should not return movements created during merges`() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              courtMovement(date = yesterday, returnDate = yesterday.plusMinutes(1))
              tapMovement(date = yesterday.plusMinutes(2), returnDate = yesterday.plusMinutes(3))
              prisonTransfer(from = "BXI", to = "MDI", date = yesterday.plusMinutes(4), arrivalDate = yesterday.plusMinutes(5))
            }
          }
        }

        // Movements 4 (TAP out), 5 (TAP in) and 6 (transfer out) were created by a merge
        repository.runInTransaction {
          entityManager.createNativeQuery(
            """
              update OFFENDER_EXTERNAL_MOVEMENTS
              set CREATE_USER_ID = 'SYS', AUDIT_MODULE_NAME = 'MERGE'
              where OFFENDER_BOOK_ID = ${booking.bookingId} and MOVEMENT_SEQ in (4, 5, 6)
            """.trimIndent(),
          ).executeUpdate()
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movements.map { it.sequence }).containsExactlyInAnyOrder(1, 2, 3, 7)
          }
      }
    }

    @Nested
    inner class Ordering {

      @Test
      fun `should return movements in sequence order`() {
        nomisDataBuilder.build {
          offender = offender {
            booking = booking(bookingBeginDate = twoDaysAgo) {
              tapApplication {
                tapScheduleOut(fromPrison = "BXI", toAgency = "HAZLWD") {
                  tapMovementOut(date = yesterday)
                  tapScheduleIn(toPrison = "BXI") {
                    tapMovementIn(date = yesterday.plusMinutes(10))
                  }
                }
              }
              courtMovement(date = yesterday.plusMinutes(20), returnDate = yesterday.plusMinutes(30))
            }
          }
        }

        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            assertThat(bookings[0].movements.map { it.sequence }).containsExactly(1, 2, 3, 4, 5)
          }
      }
    }

    @Nested
    inner class Validation {

      @Test
      fun `should return not found if offender unknown`() {
        webTestClient.getOffenderPersonLocations(offenderNo = "UNKNOWN")
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class QueryPerformance {

      @Test
      fun `should not run additional queries for multiple movements (no N+1 queries)`() {
        buildOffender(movementCount = 1)
        val statementsForOneMovement = getPreparedStatements()
        repository.deleteOffenders()

        buildOffender(movementCount = 10)
        val statementsForTenMovements = getPreparedStatements()

        assertThat(statementsForTenMovements - statementsForOneMovement)
          .withFailMessage(
            "Expected the query count to stay roughly constant as the number of movements grows " +
              "(1 took $statementsForOneMovement queries, 10 took $statementsForTenMovements queries) - " +
              "this looks like an N+1 query problem.",
          )
          .isLessThanOrEqualTo(2)
      }

      private fun buildOffender(movementCount: Int) {
        nomisDataBuilder.build {
          offender = offender {
            val addresses = (0 until movementCount).map { address(street = "Street $it", postcode = "S$it 1AA") }
            booking = booking(bookingBeginDate = twoDaysAgo) {
              repeat(movementCount) {
                tapApplication(toAddress = addresses[it]) {
                  tapScheduleOut(fromPrison = "BXI", toAddress = addresses[it], eventDate = twoDaysAgo.toLocalDate()) {
                    tapMovementOut(date = twoDaysAgo.plusMinutes(it * 10L))
                    tapScheduleIn(toPrison = "BXI") {
                      tapMovementIn(date = twoDaysAgo.plusMinutes(it * 10L + 5))
                    }
                  }
                }
                tapMovement(toAgency = "HAZLWD", date = yesterday.plusMinutes(it * 10L), returnDate = yesterday.plusMinutes(it * 10L + 5))
                courtMovement(date = yesterday.plusMinutes(it * 10L + 6), returnDate = yesterday.plusMinutes(it * 10L + 7))
              }
            }
          }
        }
      }

      private fun getPreparedStatements(): Long {
        val statistics = entityManager.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        webTestClient.getOffenderPersonLocationsOk()

        return statistics.prepareStatementCount
      }
    }

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/A1234BC/person-locations")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/person-locations")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/person-locations")
          .headers(setAuthorisation(roles = listOf("ROLE_INVALID")))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }

  private fun BookingPersonLocations.movementWithSeq(sequence: Int) = movements.single { it.sequence == sequence }

  private fun WebTestClient.getOffenderPersonLocations(
    offenderNo: String = offender.nomsId,
  ) = get()
    .uri("/movements/$offenderNo/person-locations")
    .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
    .exchange()

  private fun WebTestClient.getOffenderPersonLocationsOk(
    offenderNo: String = offender.nomsId,
  ) = getOffenderPersonLocations(offenderNo)
    .expectStatus().isOk
    .expectBodyResponse<OffenderPersonLocationsResponse>()
}
