package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.personlocation

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase

class PersonLocationBookingResourceIntTest : IntegrationTestBase() {

  @Nested
  @DisplayName("GET /movements/{offenderNo}/person-locations/booking/{bookingId}")
  inner class GetPersonLocationBooking {

    @Nested
    inner class Security {
      @Test
      fun `should return unauthorised for missing token`() {
        webTestClient.get()
          .uri("/movements/A1234BC/person-locations/booking/12345")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `should return forbidden for missing role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/person-locations/booking/12345")
          .headers(setAuthorisation())
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `should return forbidden for wrong role`() {
        webTestClient.get()
          .uri("/movements/A1234BC/person-locations/booking/12345")
          .headers(setAuthorisation("ROLE_INVALID"))
          .exchange()
          .expectStatus().isForbidden
      }
    }
  }
}
