package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonerprofile.api.PrisonerPhysicalAttributesResponse
import java.time.LocalDate

class PrisonerProfileIntTest : IntegrationTestBase() {
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
      val today = LocalDate.now()
      val yesterday = today.minusDays(1)

      @Test
      fun `should return physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
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
              assertThat(bookings).extracting("bookingId", "startDate", "endDate")
                .containsExactly(tuple(booking.bookingId, booking.bookingBeginDate.toLocalDate(), booking.bookingEndDate?.toLocalDate()))
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
            oldBooking = booking(bookingBeginDate = today.minusDays(2).atStartOfDay()) {
              physicalAttributes(180, null, null, 80, null)
              release(date = yesterday.atStartOfDay())
            }
            booking = booking(bookingBeginDate = today.atStartOfDay()) {
              physicalAttributes(170, null, null, 70, null)
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDate", "endDate")
                .containsExactly(
                  tuple(oldBooking.bookingId, today.minusDays(2), yesterday),
                  tuple(booking.bookingId, today, null),
                )
              assertThat(bookings[0].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(180, 80))
              assertThat(bookings[1].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(170, 70))
            }
          }
      }

      @Test
      fun `should return physical attributes from aliases`() {
        lateinit var aliasBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            alias {
              aliasBooking = booking(bookingBeginDate = today.minusDays(2).atStartOfDay()) {
                physicalAttributes(180, null, null, 80, null)
                release(date = yesterday.atStartOfDay())
              }
            }
            booking = booking {
              physicalAttributes(170, null, null, 70, null)
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDate", "endDate")
                .containsExactly(
                  tuple(aliasBooking.bookingId, today.minusDays(2), yesterday),
                  tuple(booking.bookingId, booking.bookingBeginDate.toLocalDate(), null),
                )
              assertThat(bookings[0].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(180, 80))
              assertThat(bookings[1].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(170, 70))
            }
          }
      }

      @Test
      fun `should return physical attributes for multiple physical attributes from multiple bookings`() {
        lateinit var oldBooking: OffenderBooking
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            oldBooking = booking(bookingBeginDate = today.minusDays(2).atStartOfDay()) {
              physicalAttributes(170, null, null, 70, null)
              physicalAttributes(171, null, null, 71, null)
              release(date = yesterday.atStartOfDay())
            }
            booking = booking(bookingBeginDate = yesterday.atStartOfDay()) {
              physicalAttributes(180, null, null, 80, null)
              physicalAttributes(181, null, null, 81, null)
              release(date = today.atStartOfDay())
            }
          }
        }

        webTestClient.getPhysicalAttributesOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(bookings).extracting("bookingId", "startDate", "endDate")
                .containsExactly(
                  tuple(oldBooking.bookingId, today.minusDays(2), yesterday),
                  tuple(booking.bookingId, yesterday, today),
                )
              assertThat(bookings[0].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(170, 70), tuple(171, 71))
              assertThat(bookings[1].physicalAttributes)
                .extracting("heightCentimetres", "weightKilograms")
                .containsExactly(tuple(180, 80), tuple(181, 81))
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
}
