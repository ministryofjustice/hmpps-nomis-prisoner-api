package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.court

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase

class OffenderCourtMovementsResourceIntTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /movements/{offenderNo}/court")
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
