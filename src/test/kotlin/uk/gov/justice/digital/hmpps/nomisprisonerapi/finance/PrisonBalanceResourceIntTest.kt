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
class PrisonBalanceResourceIntTest : IntegrationTestBase() {
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
  @DisplayName("GET /finance/prison/{prisonId}/balance")
  inner class PrisonBalanceTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prison/MDI/balance")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prison/MDI/balance")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prison/MDI/balance")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonBalance() {
      webTestClient.get().uri("/finance/prison/MDI/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_FINANCE")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.prisonId").isEqualTo("MDI")
        .jsonPath("$.accountBalances[0].accountCode").isEqualTo(2101)
        .jsonPath("$.accountBalances[0].accountPeriodId").isEqualTo(202604)
        .jsonPath("$.accountBalances[0].balance").isEqualTo("33.12")
    }

    @Test
    @Disabled
    fun `none found`() {
      webTestClient.get().uri("/finance/prison/XXX/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_FINANCE")))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }
}
