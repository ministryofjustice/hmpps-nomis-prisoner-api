package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase

class PrisonResourceIntTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /prisons")
  inner class GetPrisonPrisons {

    @Nested
    inner class Security {
      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/prisons")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/prisons")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/prisons")
          .headers(setAuthorisation("ROLE_BANANAS"))
          .exchange()
          .expectStatus().isForbidden
      }
    }

    @Test
    fun `should retrieve prisons`() {
      webTestClient.get()
        .uri("/prisons")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<List<Prison>> {
          assertThat(it).extracting("id").containsExactly("BMI", "BXI", "LEI", "MDI", "MUL", "RNI", "SYI", "TRO", "WAI")
        }
    }
  }

  @Nested
  @DisplayName("GET /prisons/{prisonId}/incentive-levels")
  inner class GetPrisonIncentiveLevels {

    @Test
    fun `should return unauthorised for missing token`() {
      webTestClient.get()
        .uri("/prisons/MDI/incentive-levels")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden for missing role`() {
      webTestClient.get()
        .uri("/prisons/MDI/incentive-levels")
        .headers(setAuthorisation())
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden for wrong role`() {
      webTestClient.get()
        .uri("/prisons/MDI/incentive-levels")
        .headers(setAuthorisation("ROLE_INVALID"))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return not found if prison not found`() {
      webTestClient.get()
        .uri("/prisons/XXX/incentive-levels")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isNotFound
        .expectBody().jsonPath("userMessage").value<String> {
          assertThat(it).contains("Prison XXX does not exist")
        }
    }

    @Test
    fun `should retrieve incentive levels`() {
      webTestClient.get()
        .uri("/prisons/MDI/incentive-levels")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<List<IncentiveLevel>> {
          assertThat(it).extracting("code").containsExactlyInAnyOrder("ENT", "BAS", "STD", "ENH")
        }
    }

    @Test
    fun `should ignore inactive incentive levels`() {
      webTestClient.get()
        .uri("/prisons/BMI/incentive-levels")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<List<IncentiveLevel>> {
          assertThat(it).extracting("code").containsExactlyInAnyOrder("BAS", "STD", "ENH")
        }
    }
  }
}
