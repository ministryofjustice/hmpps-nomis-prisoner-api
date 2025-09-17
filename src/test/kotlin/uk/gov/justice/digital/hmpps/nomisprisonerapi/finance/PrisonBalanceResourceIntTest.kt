package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.math.BigDecimal
import java.time.LocalDateTime

@WithMockAuthUser
class PrisonBalanceResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  @BeforeEach
  fun setUp() {
    nomisDataBuilder.build {
      caseloadCurrentAccountBase(caseloadId = "LEI", currentBalance = BigDecimal("23.45")) {
        transaction(currentBalance = BigDecimal("12.23"), createDateTime = LocalDateTime.parse("2019-09-09T04:03:02"))
        transaction(currentBalance = BigDecimal("11.22"), createDateTime = LocalDateTime.parse("2020-09-09T04:03:02"))
      }
      caseloadCurrentAccountBase(caseloadId = "LEI", currentBalance = BigDecimal("5.67"), accountCode = 2102) {
        transaction(currentBalance = BigDecimal("9.12"), createDateTime = LocalDateTime.parse("2020-10-09T04:03:02"))
      }
      caseloadCurrentAccountBase(caseloadId = "MDI", currentBalance = BigDecimal("5.67")) {
        transaction(currentBalance = BigDecimal("12.23"), createDateTime = LocalDateTime.parse("2019-09-09T04:03:02"))
      }
    }
  }

  @AfterEach
  fun tearDown() {
    repository.deleteAllPrisonBalances()
  }

  @Nested
  @DisplayName("GET /finance/prison/ids")
  inner class PrisonIdsTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prison/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prison/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prison/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getIds() {
      webTestClient.get().uri("/finance/prison/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$[0]").isEqualTo("LEI")
        .jsonPath("$[1]").isEqualTo("MDI")
        .jsonPath("$.length()").isEqualTo(2)
    }
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
    fun getPrisonBalanceLeeds() {
      webTestClient.get().uri("/finance/prison/LEI/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.prisonId").isEqualTo("LEI")
        .jsonPath("$.accountBalances[0].accountCode").isEqualTo(2101)
        .jsonPath("$.accountBalances[0].balance").isEqualTo("23.45")
        .jsonPath("$.accountBalances[0].transactionDate").isEqualTo("2020-09-09T04:03:02")
        .jsonPath("$.accountBalances[1].accountCode").isEqualTo(2102)
        .jsonPath("$.accountBalances[1].balance").isEqualTo("9.12")
        .jsonPath("$.accountBalances[1].transactionDate").isEqualTo("2020-10-09T04:03:02")
        .jsonPath("$.accountBalances.length()").isEqualTo(2)
    }

    @Test
    fun getPrisonBalanceMoorland() {
      webTestClient.get().uri("/finance/prison/MDI/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.prisonId").isEqualTo("MDI")
        .jsonPath("$.accountBalances[0].accountCode").isEqualTo(2101)
        .jsonPath("$.accountBalances[0].balance").isEqualTo("12.23")
        .jsonPath("$.accountBalances[0].transactionDate").isEqualTo("2019-09-09T04:03:02")
        .jsonPath("$.accountBalances.length()").isEqualTo(1)
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/finance/prison/XXX/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$.prisonId").isEqualTo("XXX")
        .jsonPath("$.accountBalances.length()").isEqualTo(0)
    }
  }
}
