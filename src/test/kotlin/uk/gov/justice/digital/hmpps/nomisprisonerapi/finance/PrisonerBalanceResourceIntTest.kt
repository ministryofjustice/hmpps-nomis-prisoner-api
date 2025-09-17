package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser

@WithMockAuthUser
class PrisonerBalanceResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setUp() {
    nomisDataBuilder.build {}
  }

  @AfterEach
  fun tearDown() {
  }

  @Nested
  @DisplayName("GET /finance/prisoners/ids")
  inner class PrisonerIdsTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerIds() {
      webTestClient.get().uri("/finance/prisoners/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(2)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("content[0]").isEqualTo("12345")
        .jsonPath("content[1]").isEqualTo("67890")
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/{prisonerId}/balance")
  inner class PrisonerBalanceTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/12345/balance")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/12345/balance")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/12345/balance")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerBalance() {
      webTestClient.get().uri("/finance/prisoners/12345/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("prisonerId").isEqualTo("12345")
        .jsonPath("accounts.length()").isEqualTo(3)
        .jsonPath("accounts[0].prisonId").isEqualTo("MDI")
        .jsonPath("accounts[0].lastTransactionId").isEqualTo(56789)
        .jsonPath("accounts[0].subAccountType").isEqualTo("CASH")
        .jsonPath("accounts[0].balance").isEqualTo("12.5")
        .jsonPath("accounts[0].holdBalance").doesNotExist()
        .jsonPath("accounts[2].holdBalance").isEqualTo("2.5")
    }

    // TODO reintroduce test once service & repository code complete
    @Test
    @Disabled
    fun `none found`() {
      webTestClient.get().uri("/finance/prisoners/99999/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }
}
