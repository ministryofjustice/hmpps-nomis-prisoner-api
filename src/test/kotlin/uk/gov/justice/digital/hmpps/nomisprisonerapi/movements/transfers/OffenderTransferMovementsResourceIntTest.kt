package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase

class OffenderTransferMovementsResourceIntTest : IntegrationTestBase() {

  @Nested
  inner class GetOffenderTransferMovements {

    @Nested
    inner class Security {

      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/A1234BC/transfer")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/transfer")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/transfer")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
