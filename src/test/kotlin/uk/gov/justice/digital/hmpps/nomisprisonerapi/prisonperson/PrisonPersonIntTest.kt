package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.api.PrisonerPhysicalAttributesResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class PrisonPersonIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /prisoners/{offenderNo}/physical-attributes")
  inner class GetPhysicalAttributes {
    @Nested
    inner class Security {
      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `not found`() {
        webTestClient.get().uri("/prisoners/A1234AA/physical-attributes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_PROFILE")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      lateinit var booking: OffenderBooking

      // The DB column is a DATE type so truncates milliseconds, but bizarrely H2 uses half-up rounding so I have to emulate here or tests fail
      val today = LocalDateTime.now().roundToNearestSecond()
      val yesterday = today.minusDays(1)

      @Test
      fun `should return physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              physicalAttributes(
                heightCentimetres = 180,
                weightKilograms = 81,
              )
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(tuple(booking.bookingId, booking.bookingBeginDate, booking.bookingEndDate, true))
              assertThat(bookings[0].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(180, 81))
            }
          }
      }

      @Test
      fun `should not return bookings without physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking()
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(bookings).isEmpty()
            }
          }
      }

      @Test
      fun `should return multiple physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(180, null, null, weightKilograms = 80, null)
              physicalAttributes(170, null, null, weightKilograms = 70, null)
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings[0].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(
                  tuple(180, 80),
                  tuple(170, 70),
                )
            }
          }
      }

      @Test
      fun `should return physical attributes from old bookings`() {
        lateinit var oldBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              physicalAttributes(170, null, null, 70, null)
            }
            oldBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              physicalAttributes(180, null, null, 80, null)
              release(date = yesterday)
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(
                  tuple(booking.bookingId, today, null, true),
                  tuple(oldBooking.bookingId, today.minusDays(2), yesterday, false),
                )
              assertThat(bookings[0].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(170, 70))
              assertThat(bookings[1].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(180, 80))
            }
          }
      }

      @Test
      fun `should return physical attributes from aliases`() {
        lateinit var aliasBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              physicalAttributes(170, null, null, 70, null)
            }
            alias {
              aliasBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
                physicalAttributes(180, null, null, 80, null)
                release(date = yesterday)
              }
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(
                  tuple(booking.bookingId, booking.bookingBeginDate, null, true),
                  tuple(aliasBooking.bookingId, today.minusDays(2), yesterday, false),
                )
              assertThat(bookings[0].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(170, 70))
              assertThat(bookings[1].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(180, 80))
            }
          }
      }

      @Test
      fun `should return physical attributes for multiple physical attributes from multiple bookings`() {
        lateinit var oldBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = yesterday) {
              physicalAttributes(180, null, null, 80, null)
              physicalAttributes(181, null, null, 81, null)
              release(date = today)
            }
            oldBooking = booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              physicalAttributes(170, null, null, 70, null)
              physicalAttributes(171, null, null, 71, null)
              release(date = yesterday)
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDateTime", "endDateTime", "latestBooking")
                .containsExactly(
                  tuple(booking.bookingId, yesterday, today, true),
                  tuple(oldBooking.bookingId, today.minusDays(2), yesterday, false),
                )
              assertThat(bookings[0].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(180, 80), tuple(181, 81))
              assertThat(bookings[1].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(170, 70), tuple(171, 71))
            }
          }
      }
    }
  }

  fun WebTestClient.getPhysicalAttributesOk(offenderNo: String) =
    this.get().uri("/prisoners/$offenderNo/physical-attributes")
      .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_PROFILE")))
      .exchange()
      .expectStatus().isOk
      .expectBody<PrisonerPhysicalAttributesResponse>()

  private fun LocalDateTime.roundToNearestSecond(): LocalDateTime {
    val secondsOnly = this.truncatedTo(ChronoUnit.SECONDS)
    val nanosOnly = this.nano
    val nanosRounded = if (nanosOnly >= 500_000_000) 1 else 0
    return secondsOnly.plusSeconds(nanosRounded.toLong())
  }
}
