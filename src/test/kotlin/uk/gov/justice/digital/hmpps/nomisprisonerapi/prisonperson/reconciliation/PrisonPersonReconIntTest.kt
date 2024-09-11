package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisonperson.reconciliation

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
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
import java.time.LocalDateTime

class PrisonPersonReconIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /prisoners/{offenderNo}/prison-person/reconciliation")
  inner class GetPrisonPersonReconciliation {
    lateinit var booking: OffenderBooking

    @Nested
    inner class Security {
      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234AA/prison-person/reconciliation")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234AA/prison-person/reconciliation")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234AA/prison-person/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `not found if prisoner does not exist`() {
        webTestClient.get().uri("/prisoners/A1234AA/prison-person/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `not found if no bookings`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
          }
        }

        webTestClient.get().uri("/prisoners/A1234AA/prison-person/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class PhysicalAttributes {
      val today: LocalDateTime = LocalDateTime.now()
      val yesterday: LocalDateTime = today.minusDays(1)

      @Test
      fun `should return physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingBeginDate = yesterday) {
              physicalAttributes(
                heightCentimetres = 180,
                weightKilograms = 80,
              )
            }
          }
        }

        webTestClient.getReconciliationOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(height).isEqualTo(180)
              assertThat(weight).isEqualTo(80)
            }
          }
      }

      @Test
      fun `should return nulls if no physical attributes`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking()
          }
        }

        webTestClient.getReconciliationOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(height).isNull()
              assertThat(weight).isNull()
            }
          }
      }

      @Test
      fun `should return first attribute sequence from active booking`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(180, null, null, weightKilograms = 80, null, sequence = 1L)
              physicalAttributes(170, null, null, weightKilograms = 70, null, sequence = 2L)
            }
          }
        }

        webTestClient.getReconciliationOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(height).isEqualTo(180)
              assertThat(weight).isEqualTo(80)
            }
          }
      }

      @Test
      fun `should return first attribute sequence from active booking even if not seq=1`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(180, null, null, weightKilograms = 80, null, sequence = 2L)
              physicalAttributes(170, null, null, weightKilograms = 70, null, sequence = 3L)
            }
          }
        }

        webTestClient.getReconciliationOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(height).isEqualTo(180)
              assertThat(weight).isEqualTo(80)
            }
          }
      }

      @Test
      fun `should return physical attributes from latest booking`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              physicalAttributes(170, null, null, 70, null)
            }
            booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              physicalAttributes(180, null, null, 80, null)
              release(date = yesterday)
            }
          }
        }

        webTestClient.getReconciliationOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(height).isEqualTo(170)
              assertThat(weight).isEqualTo(70)
            }
          }
      }

      @Test
      fun `should return null if no physical attributes on active booking`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              // no physical attributes on active booking
            }
            booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              physicalAttributes(180, null, null, 80, null)
              release(date = yesterday)
            }
          }
        }

        webTestClient.getReconciliationOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(height).isEqualTo(null)
              assertThat(weight).isEqualTo(null)
            }
          }
      }

      @Test
      fun `should return physical attributes from lowest booking sequence if multiple bookings have null end date`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking(bookingSequence = 2, bookingBeginDate = today.minusDays(2)) {
              physicalAttributes(170, null, null, 70, null)
            }
            booking = booking(bookingSequence = 1, bookingBeginDate = today) {
              physicalAttributes(180, null, null, 80, null)
            }
          }
        }

        webTestClient.getReconciliationOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(height).isEqualTo(180)
              assertThat(weight).isEqualTo(80)
            }
          }
      }

      @Test
      fun `should convert from imperial to metric if metric measures are empty`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(
                heightFeet = 5,
                heightInches = 10,
                weightPounds = 180,
              )
            }
          }
        }

        webTestClient.getReconciliationOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(height).isEqualTo(180)
              assertThat(weight).isEqualTo(82)
            }
          }
      }

      @Test
      fun `should return metric height if both imperial and metric measures are present`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(
                heightCentimetres = 180,
                heightFeet = 5,
                heightInches = 10,
              )
            }
          }
        }

        webTestClient.getReconciliationOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(height).isEqualTo(180)
            }
          }
      }

      @Test
      fun `should convert from imperial weight if both imperial and metric measures are present`() {
        nomisDataBuilder.build {
          offender(nomsId = "A1234AA") {
            booking = booking {
              physicalAttributes(
                weightKilograms = 80,
                weightPounds = 180,
              )
            }
          }
        }

        /*
         * Note this result is different to the weightKilograms on the NOMIS record (80).
         * This is because we know the user entered weightPounds, so we convert that to kilograms.
         * We know the user entered weightPounds because had they entered weightKilograms, weightPounds would be 80/0.45359=176.37, clearly not 180.
         */
        webTestClient.getReconciliationOk("A1234AA")
          .consumeWith {
            with(it.responseBody!!) {
              assertThat(offenderNo).isEqualTo("A1234AA")
              assertThat(weight).isEqualTo(82)
            }
          }
      }
    }

    fun WebTestClient.getReconciliationOk(offenderNo: String) =
      this.get().uri("/prisoners/$offenderNo/prison-person/reconciliation")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISON_PERSON")))
        .exchange()
        .expectStatus().isOk
        .expectBody<PrisonPersonReconciliationResponse>()
  }
}
