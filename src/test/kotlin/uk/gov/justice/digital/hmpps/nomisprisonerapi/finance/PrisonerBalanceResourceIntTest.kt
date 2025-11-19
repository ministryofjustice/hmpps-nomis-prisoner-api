package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
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
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

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
      offender()
      id1 = offender(nomsId = "A1234BC") {
        trustAccount()
        trustAccount(caseloadId = "LEI", currentBalance = BigDecimal.valueOf(12.50)) {
          subAccount(accountCode = 2102, balance = BigDecimal.valueOf(11.25), lastTransactionId = 45678)
          subAccount(accountCode = 2103, balance = BigDecimal.valueOf(1.25), lastTransactionId = 67890)
        }
        trustAccount(caseloadId = "WWI", currentBalance = BigDecimal.valueOf(-1.50)) {
          subAccount(accountCode = 2102, balance = BigDecimal.valueOf(-1.50), holdBalance = BigDecimal.ZERO, lastTransactionId = 56789)
        }
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
    fun getPrisonerIdsAtMultiplePrisons() {
      webTestClient.get().uri("/finance/prisoners/ids?prisonId=WWI&prisonId=LEI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("page.totalElements").isEqualTo(2)
        .jsonPath("content[0]").isEqualTo(id1)
        .jsonPath("content[1]").isEqualTo(id2)
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
  @DisplayName("GET /finance/prisoners/ids/all-from-id")
  inner class PrisonerIdsFromLastTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/ids/all-from-id")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/ids/all-from-id")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/ids/all-from-id")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun `will return first page of prisoners ordered by rootOffenderId ASC`() {
      webTestClient.get().uri("/finance/prisoners/ids/all-from-id?pageSize=2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("rootOffenderIds.size()").isEqualTo("2")
        .jsonPath("rootOffenderIds[0]").isEqualTo(id1)
        .jsonPath("rootOffenderIds[1]").isEqualTo(id2)
        .jsonPath("lastOffenderId").isEqualTo(id2)
    }

    @Test
    fun `will return second page of prisoners ordered by rootOffenderId ASC`() {
      webTestClient.get().uri("/finance/prisoners/ids/all-from-id?pageSize=2&offenderId=$id2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("rootOffenderIds[0]").isEqualTo(id1)
        .jsonPath("rootOffenderIds[1]").isEqualTo(id2)
        .jsonPath("lastOffenderId").isEqualTo(id2)
    }

    @Test
    fun `will allow return of no match if nothing returned for selected prison`() {
      webTestClient.get().uri("/finance/prisoners/ids/all-from-id?pageSize=2&prisonId=BMI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("rootOffenderIds.size()").isEqualTo("0")
        .jsonPath("lastOffenderId").isEqualTo("0")
    }

    @Test
    fun `will allow single prisonId query param`() {
      webTestClient.get().uri("/finance/prisoners/ids/all-from-id?pageSize=2&prisonId=WWI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("rootOffenderIds.size()").isEqualTo("1")
        .jsonPath("rootOffenderIds[0]").isEqualTo(id1)
        .jsonPath("lastOffenderId").isEqualTo(id1)
    }

    @Test
    fun `will allow multiple prisonId query params`() {
      webTestClient.get().uri("/finance/prisoners/ids/all-from-id?pageSize=10&prisonId=WWI&prisonId=LEI&prisonId=MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("rootOffenderIds.size()").isEqualTo("3")
        .jsonPath("rootOffenderIds[0]").isEqualTo(id1)
        .jsonPath("rootOffenderIds[1]").isEqualTo(id2)
        .jsonPath("rootOffenderIds[2]").isEqualTo(id3)
        .jsonPath("lastOffenderId").isEqualTo(id3)
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
    fun getPrisonerBalanceWithDifferentPrisons() {
      val balance = webTestClient.get().uri("/finance/prisoners/$id1/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerBalanceDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id1)
        assertThat(prisonNumber).isEqualTo("A1234BC")
        assertThat(accounts.size).isEqualTo(3)
        assertThat(accounts[0].prisonId).isEqualTo("LEI")
        assertThat(accounts[0].accountCode).isEqualTo(2102)
        assertThat(accounts[0].balance).isEqualTo(BigDecimal(11.25))
        assertThat(accounts[0].holdBalance).isNull()
        assertThat(accounts[0].lastTransactionId).isEqualTo(45678)
        assertThat(accounts[0].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))

        assertThat(accounts[1].prisonId).isEqualTo("LEI")
        assertThat(accounts[1].accountCode).isEqualTo(2103)
        assertThat(accounts[1].balance).isEqualTo(BigDecimal(1.25))
        assertThat(accounts[1].holdBalance).isNull()
        assertThat(accounts[1].lastTransactionId).isEqualTo(67890)
        assertThat(accounts[0].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))

        assertThat(accounts[2].prisonId).isEqualTo("WWI")
        assertThat(accounts[2].accountCode).isEqualTo(2102)
        assertThat(accounts[2].balance).isEqualTo("-1.5")
        assertThat(accounts[2].holdBalance).isEqualTo(BigDecimal.ZERO)
        assertThat(accounts[2].lastTransactionId).isEqualTo(56789)
        assertThat(accounts[2].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
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
        assertThat(accounts[0].accountCode).isEqualTo(2101)
        assertThat(accounts[0].balance).isEqualTo(BigDecimal(0))
        assertThat(accounts[0].holdBalance).isNull()
        assertThat(accounts[0].lastTransactionId).isEqualTo(12345)
        assertThat(accounts[0].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        assertThat(accounts[1].prisonId).isEqualTo("LEI")
        assertThat(accounts[1].accountCode).isEqualTo(2102)
        assertThat(accounts[1].holdBalance).isEqualTo(BigDecimal(0))
        assertThat(accounts[1].lastTransactionId).isEqualTo(34567)
        assertThat(accounts[1].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        assertThat(accounts[2].prisonId).isEqualTo("LEI")
        assertThat(accounts[2].accountCode).isEqualTo(2103)
        assertThat(accounts[2].balance).isEqualTo("21.25")
        assertThat(accounts[2].holdBalance).isEqualTo("2.5")
        assertThat(accounts[2].lastTransactionId).isEqualTo(56789)
        assertThat(accounts[2].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
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
