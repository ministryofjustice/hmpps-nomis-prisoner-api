package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.math.BigDecimal

@WithMockAuthUser
class PrisonerBalanceResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  private var id1: Long = 0
  private var id2: Long = 0
  private var id3: Long = 0

  @BeforeEach
  fun setUp() {
    nomisDataBuilder.build {
      id1 = offender {
        trustAccount()
        trustAccount(caseloadId = "LEI", currentBalance = BigDecimal.valueOf(12.50))
        trustAccount(caseloadId = "WWI", currentBalance = BigDecimal.valueOf(-1.50))
      }.id
      id2 = offender {
        trustAccount()
        trustAccount(caseloadId = "LEI", currentBalance = BigDecimal.valueOf(33.50))
      }.id
      id3 = offender {
        trustAccount(holdBalance = BigDecimal.valueOf(1.25))
      }.id
      offender {
        trustAccount()
      }
    }
  }

  @AfterEach
  fun tearDown() {
    repository.deleteOffenders()
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
        .jsonPath("page.totalElements").isEqualTo(3)
        .jsonPath("content[0]").isEqualTo(id1)
        .jsonPath("content[1]").isEqualTo(id2)
        .jsonPath("content[2]").isEqualTo(id3)
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
        .jsonPath("rootOffenderId").isEqualTo("12345")
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
