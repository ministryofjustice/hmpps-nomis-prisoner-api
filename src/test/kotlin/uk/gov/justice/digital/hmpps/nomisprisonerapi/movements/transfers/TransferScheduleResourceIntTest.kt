package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase

class TransferScheduleResourceIntTest : IntegrationTestBase() {

  private val offenderNo = "B7463BB"

  @Nested
  @DisplayName("GET /movements/{offenderNo}/transfers/schedule/out/{eventId}")
  inner class GetTransferScheduleOut {

    @Nested
    inner class Security {
      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/transfers/schedule/out/1")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/transfers/schedule/out/1")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/$offenderNo/transfers/schedule/out/1")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
