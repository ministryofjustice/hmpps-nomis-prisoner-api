package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners.expectBodyResponse
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
      id1 = offender(nomsId = "A1234BC") {
        trustAccount()
        trustAccount(caseloadId = "LEI", currentBalance = BigDecimal.valueOf(12.50)) {
          subAccount(accountCode = 2102, balance = BigDecimal.valueOf(11.25), lastTransactionId = 45678)
        }
        trustAccount(caseloadId = "WWI", currentBalance = BigDecimal.valueOf(-1.50))
      }.id
      id2 = offender(nomsId = "B2345CD") {
        trustAccount()
        trustAccount(caseloadId = "LEI", currentBalance = BigDecimal.valueOf(34.50)) {
          subAccount()
          subAccount(accountCode = 2102, balance = BigDecimal.valueOf(12.25), holdBalance = BigDecimal.ZERO, lastTransactionId = 34567)
          subAccount(accountCode = 2103, balance = BigDecimal.valueOf(21.25), holdBalance = BigDecimal.valueOf(2.50), lastTransactionId = 56789)
        }
      }.id
      id3 = offender(nomsId = "C3456DE") {
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

    @Test
    fun getPrisonerIdsAtPrison() {
      webTestClient.get().uri("/finance/prisoners/ids?prisonId=WWI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("page.totalElements").isEqualTo(1)
        .jsonPath("content[0]").isEqualTo(id1)
    }

    @Test
    fun getPrisonerIdsAtPrisonNoResults() {
      webTestClient.get().uri("/finance/prisoners/ids?prisonId=XXI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("page.totalElements").isEqualTo(0)
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
      val balance = webTestClient.get().uri("/finance/prisoners/$id2/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerBalanceDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id2)
        assertThat(prisonNumber).isEqualTo("B2345CD")
        assertThat(accounts.size).isEqualTo(3)
        assertThat(accounts[0].prisonId).isEqualTo("LEI")
        assertThat(accounts[0].lastTransactionId).isEqualTo(12345)
        assertThat(accounts[0].accountCode).isEqualTo(2101)
        assertThat(accounts[0].balance).isEqualTo(BigDecimal(0))
        assertThat(accounts[0].holdBalance).isNull()
        assertThat(accounts[1].prisonId).isEqualTo("LEI")
        assertThat(accounts[1].accountCode).isEqualTo(2102)
        assertThat(accounts[1].holdBalance).isEqualTo(BigDecimal(0))
        assertThat(accounts[2].prisonId).isEqualTo("LEI")
        assertThat(accounts[2].accountCode).isEqualTo(2103)
        assertThat(accounts[2].balance).isEqualTo("21.25")
        assertThat(accounts[2].holdBalance).isEqualTo("2.5")
      }
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/finance/prisoners/99999/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }
}
