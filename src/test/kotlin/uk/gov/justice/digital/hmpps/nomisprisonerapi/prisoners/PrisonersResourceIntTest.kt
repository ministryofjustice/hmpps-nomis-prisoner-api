package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender

class PrisonersResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  @Nested
  @DisplayName("GET /prisoners/ids")
  inner class GetPrisoners {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested inner class HappyPath {
      lateinit var activePrisoner1: Offender
      lateinit var activePrisoner2: Offender
      lateinit var inactivePrisoner1: Offender

      @BeforeEach
      internal fun createPrisoner() {
        activePrisoner1 = repository.save(
          OffenderBuilder(nomsId = "A1234TT")
            .withBooking(OffenderBookingBuilder()),
        )
        activePrisoner2 = repository.save(
          OffenderBuilder(nomsId = "A1234SS")
            .withBooking(OffenderBookingBuilder()),
        )
        inactivePrisoner1 = repository.save(
          OffenderBuilder(nomsId = "A1234WW")
            .withBooking(OffenderBookingBuilder(active = false)),
        )
      }

      @AfterEach
      fun tearDown() {
        repository.deleteOffenders()
      }

      @Test
      fun `will return count of all active prisoners by default`() {
        webTestClient.get().uri("/prisoners/ids?size=1&page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.totalElements").isEqualTo(2)
          .jsonPath("$.numberOfElements").isEqualTo(1)
      }

      @Test
      fun `finding all prisoners (including inactive ones) is not supported currently`() {
        webTestClient.get().uri("/prisoners/ids?size=1&page=0&active=false")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().is5xxServerError
      }
    }
  }
}
