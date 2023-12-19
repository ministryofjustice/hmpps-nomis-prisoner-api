package uk.gov.justice.digital.hmpps.nomisprisonerapi.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase

class PrisonResourceIntTest : IntegrationTestBase() {

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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
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
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_ACTIVITIES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$").value<List<IncentiveLevel>> {
          assertThat(it).extracting("code").containsExactlyInAnyOrder("BAS", "STD", "ENH")
        }
    }
  }
}
