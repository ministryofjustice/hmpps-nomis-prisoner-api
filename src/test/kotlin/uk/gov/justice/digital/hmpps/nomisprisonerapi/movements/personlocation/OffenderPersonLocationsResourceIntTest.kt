package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.roundToNearestSecond
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation.offender.OffenderPersonLocationsResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class OffenderPersonLocationsResourceIntTest : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var booking: OffenderBooking
  private val now = LocalDateTime.now().roundToNearestSecond()
  private val yesterday = now.minusDays(1)

  @AfterEach
  fun `reset data`() {
    repository.deleteOffenders()
  }

  @Nested
  inner class GetOffenderPersonLocations {

    @Nested
    inner class HappyPath {

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
              with(bookings[0]) {
                assertThat(bookingId).isEqualTo(booking.bookingId)
                assertThat(bookingNumber).isEqualTo(booking.bookNumber)
                assertThat(activeBooking).isFalse
                assertThat(latestBooking).isTrue
                assertThat(bookingStatus.name).isEqualTo("OPEN")
                assertThat(bookingBeginTime).isEqualTo(yesterday)
                assertThat(bookingEndTime).isEqualTo(now.truncatedTo(ChronoUnit.DAYS))
                assertThat(movements).hasSize(2)
                assertThat(audit.createDatetime).isCloseTo(now, within(50, ChronoUnit.MINUTES))
                assertThat(audit.createUsername).isEqualTo("SA")
              }
            }
          }
      }

      @Test
      fun `should get admission details`() {
        webTestClient.getOffenderPersonLocationsOk()
          .apply {
            repository.runInTransaction {
              with(bookings[0].movements[0]) {
                assertThat(sequence).isEqualTo(1)
                assertThat(movementType.name).isEqualTo("ADM")
                assertThat(directionCode.name).isEqualTo("IN")
                assertThat(movementTime).isEqualTo(yesterday)
                assertThat(movementReason.code).isEqualTo("N")
                assertThat(movementReason.description).isEqualTo("Unconvicted Remand")
                assertThat(from!!.code).isEqualTo("ABDRCT")
                assertThat(from.description).isEqualTo("court 2")
                assertThat(from.type.name).isEqualTo("COURT")
                assertThat(to!!.code).isEqualTo("BXI")
                assertThat(to.description).isEqualTo("BRIXTON")
                assertThat(to.type.name).isEqualTo("PRISON")
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
              with(bookings[0].movements[1]) {
                assertThat(sequence).isEqualTo(2)
                assertThat(movementType.name).isEqualTo("REL")
                assertThat(directionCode.name).isEqualTo("OUT")
                assertThat(movementTime).isEqualTo(now)
                assertThat(movementReason.code).isEqualTo("CR")
                assertThat(movementReason.description).isEqualTo("Conditional Release")
                assertThat(from!!.code).isEqualTo("BXI")
                assertThat(from.description).isEqualTo("BRIXTON")
                assertThat(from.type.name).isEqualTo("PRISON")
                assertThat(to!!.code).isEqualTo("OUT")
                assertThat(to.description).isEqualTo("OUTSIDE")
                assertThat(to.type.name).isEqualTo("OTHER")
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
