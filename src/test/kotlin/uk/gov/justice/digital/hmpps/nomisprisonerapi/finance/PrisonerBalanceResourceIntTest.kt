package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.expectBodyResponse
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit.SECONDS

@WithMockAuthUser
class PrisonerBalanceResourceIntTest : IntegrationTestBase() {
  private var id1: Long = 0
  private var id2: Long = 0
  private var id3: Long = 0

  @BeforeEach
  fun setUp() {
    nomisDataBuilder.build {
      offender()
      id1 = offender(nomsId = "A1234BC") {
        booking {
          transaction(transactionId = 45678, transactionType = "DPST", entryDate = LocalDate.parse("2026-02-01")) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2102, entryTime = LocalTime.parse("11:23:54"))
            generalLedgerTransaction(generalLedgerEntrySequence = 2, accountCode = 2103, entryTime = LocalTime.parse("11:23:54"))
          }
          transaction(transactionId = 55555, transactionType = "DPST", entryDate = LocalDate.parse("2026-02-01")) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2101, entryTime = LocalTime.parse("14:10:45"))
          }
          transaction(transactionId = 66666, transactionType = "DPST", entryDate = LocalDate.parse("2026-02-02")) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2102, entryTime = LocalTime.parse("09:20:01"))
          }
        }

        trustAccount()
        trustAccount(caseloadId = "LEI", currentBalance = BigDecimal.valueOf(12.50)) {
          subAccount(accountCode = 2102, balance = BigDecimal.valueOf(11.25), lastTransactionId = 45678)
          subAccount(accountCode = 2103, balance = BigDecimal.valueOf(1.25), lastTransactionId = 45678)
        }
        trustAccount(caseloadId = "WWI", currentBalance = BigDecimal.valueOf(-1.50)) {
          subAccount(accountCode = 2101, balance = BigDecimal.ZERO, lastTransactionId = 55555)
          subAccount(accountCode = 2102, balance = BigDecimal.valueOf(-1.50), holdBalance = BigDecimal.ZERO, lastTransactionId = 66666)
        }
      }.id
      id2 = offender(nomsId = "B2345CD") {
        booking {
          transaction(transactionId = 12345, transactionType = "DPST", entryDate = LocalDate.parse("2026-04-02")) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2101, entryTime = LocalTime.parse("16:11:12"))
          }
          transaction(transactionId = 34567, transactionType = "DPST", entryDate = LocalDate.parse("2026-02-02")) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2102, entryTime = LocalTime.parse("10:00:11"))
          }
          transaction(transactionId = 56789, transactionType = "DPST", entryDate = LocalDate.parse("2026-02-02")) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2103, entryTime = LocalTime.parse("04:12:12"))
          }
        }

        trustAccount()
        trustAccount(caseloadId = "LEI", currentBalance = BigDecimal.valueOf(34.50)) {
          subAccount(accountCode = 2101, lastTransactionId = 12345)
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
    repository.deleteAllTransactions()
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

    @Test
    fun getPrisonerIdsEmptyList() {
      webTestClient.get().uri("/finance/prisoners/ids?prisonId=")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("page.totalElements").isEqualTo(3)
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
  @DisplayName("GET /finance/prisoners/rootOffenderId/{rootOffenderId}/balance-details")
  inner class PrisonerBalanceByRootOffenderIdTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/rootOffenderId/12345/balance-details")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/rootOffenderId/12345/balance-details")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/rootOffenderId/12345/balance-details")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerBalanceWithDifferentPrisons() {
      val balance = webTestClient.get().uri("/finance/prisoners/rootOffenderId/$id1/balance-details")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerBalanceDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id1)
        assertThat(prisonNumber).isEqualTo("A1234BC")
        assertThat(accounts.size).isEqualTo(4)
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
        assertThat(accounts[1].lastTransactionId).isEqualTo(45678)
        assertThat(accounts[1].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))

        assertThat(accounts[2].prisonId).isEqualTo("WWI")
        assertThat(accounts[2].accountCode).isEqualTo(2101)
        assertThat(accounts[2].balance).isEqualTo(BigDecimal.ZERO)
        assertThat(accounts[2].holdBalance).isNull()
        assertThat(accounts[2].lastTransactionId).isEqualTo(55555)
        assertThat(accounts[2].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))

        assertThat(accounts[3].prisonId).isEqualTo("WWI")
        assertThat(accounts[3].accountCode).isEqualTo(2102)
        assertThat(accounts[3].balance).isEqualTo("-1.5")
        assertThat(accounts[3].holdBalance).isEqualTo(BigDecimal.ZERO)
        assertThat(accounts[3].lastTransactionId).isEqualTo(66666)
        assertThat(accounts[3].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
      }
    }

    @Test
    fun getPrisonerBalance() {
      val balance = webTestClient.get().uri("/finance/prisoners/rootOffenderId/$id2/balance-details")
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
    fun getPrisonerBalanceExcludingZeroBalances() {
      val balance = webTestClient.get().uri("/finance/prisoners/rootOffenderId/$id2/balance-details?excludeZeroBalances=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerBalanceDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id2)
        assertThat(prisonNumber).isEqualTo("B2345CD")
        assertThat(accounts.size).isEqualTo(2)
        assertThat(accounts[0].prisonId).isEqualTo("LEI")
        assertThat(accounts[0].accountCode).isEqualTo(2102)
        assertThat(accounts[0].holdBalance).isEqualTo(BigDecimal(0))
        assertThat(accounts[0].lastTransactionId).isEqualTo(34567)
        assertThat(accounts[0].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        assertThat(accounts[1].prisonId).isEqualTo("LEI")
        assertThat(accounts[1].accountCode).isEqualTo(2103)
        assertThat(accounts[1].balance).isEqualTo("21.25")
        assertThat(accounts[1].holdBalance).isEqualTo("2.5")
        assertThat(accounts[1].lastTransactionId).isEqualTo(56789)
        assertThat(accounts[1].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
      }
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/finance/prisoners/rootOffenderId/99999/balance-details")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/{prisonNumber}/balance-details")
  inner class PrisonerBalanceByPrisonNumberTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/B2345CD/balance-details")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/B2345CD/balance-details")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/B2345CD/balance-details")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerBalanceWithDifferentPrisons() {
      val balance = webTestClient.get().uri("/finance/prisoners/A1234BC/balance-details")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerBalanceDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id1)
        assertThat(prisonNumber).isEqualTo("A1234BC")
        assertThat(accounts.size).isEqualTo(4)
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
        assertThat(accounts[1].lastTransactionId).isEqualTo(45678)
        assertThat(accounts[1].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))

        assertThat(accounts[2].prisonId).isEqualTo("WWI")
        assertThat(accounts[2].accountCode).isEqualTo(2101)
        assertThat(accounts[2].balance).isEqualTo(BigDecimal.ZERO)
        assertThat(accounts[2].holdBalance).isNull()
        assertThat(accounts[2].lastTransactionId).isEqualTo(55555)
        assertThat(accounts[2].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))

        assertThat(accounts[3].prisonId).isEqualTo("WWI")
        assertThat(accounts[3].accountCode).isEqualTo(2102)
        assertThat(accounts[3].balance).isEqualTo("-1.5")
        assertThat(accounts[3].holdBalance).isEqualTo(BigDecimal.ZERO)
        assertThat(accounts[3].lastTransactionId).isEqualTo(66666)
        assertThat(accounts[3].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
      }
    }

    @Test
    fun getPrisonerBalance() {
      val balance = webTestClient.get().uri("/finance/prisoners/B2345CD/balance-details")
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
    fun getPrisonerBalanceExcludingZeroBalances() {
      val balance = webTestClient.get().uri("/finance/prisoners/B2345CD/balance-details?excludeZeroBalances=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerBalanceDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id2)
        assertThat(prisonNumber).isEqualTo("B2345CD")
        assertThat(accounts.size).isEqualTo(2)
        assertThat(accounts[0].prisonId).isEqualTo("LEI")
        assertThat(accounts[0].accountCode).isEqualTo(2102)
        assertThat(accounts[0].holdBalance).isEqualTo(BigDecimal(0))
        assertThat(accounts[0].lastTransactionId).isEqualTo(34567)
        assertThat(accounts[0].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        assertThat(accounts[1].prisonId).isEqualTo("LEI")
        assertThat(accounts[1].accountCode).isEqualTo(2103)
        assertThat(accounts[1].balance).isEqualTo("21.25")
        assertThat(accounts[1].holdBalance).isEqualTo("2.5")
        assertThat(accounts[1].lastTransactionId).isEqualTo(56789)
        assertThat(accounts[1].transactionDate).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
      }
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/finance/prisoners/A999BC/balance-details")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/rootOffenderId/{rootOffenderId}/balance")
  inner class PrisonerBalanceMigrationByRootOffenderIdTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/rootOffenderId/12345/balance")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/rootOffenderId/12345/balance")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/rootOffenderId/12345/balance")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerBalanceWithDifferentPrisons() {
      val balance = webTestClient.get().uri("/finance/prisoners/rootOffenderId/$id1/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerBalanceDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id1)
        assertThat(prisonNumber).isEqualTo("A1234BC")
        assertThat(accounts.size).isEqualTo(4)
        assertThat(accounts[0].prisonId).isEqualTo("LEI")
        assertThat(accounts[0].accountCode).isEqualTo(2102)
        assertThat(accounts[0].balance).isEqualTo(BigDecimal(11.25))
        assertThat(accounts[0].holdBalance).isNull()
        assertThat(accounts[0].lastTransactionId).isEqualTo(45678)
        assertThat(accounts[0].transactionDate).isEqualTo(LocalDateTime.parse("2026-02-01T11:23:54"))

        assertThat(accounts[1].prisonId).isEqualTo("LEI")
        assertThat(accounts[1].accountCode).isEqualTo(2103)
        assertThat(accounts[1].balance).isEqualTo(BigDecimal(1.25))
        assertThat(accounts[1].holdBalance).isNull()
        assertThat(accounts[1].lastTransactionId).isEqualTo(45678)
        assertThat(accounts[1].transactionDate).isEqualTo(LocalDateTime.parse("2026-02-01T11:23:54"))

        assertThat(accounts[2].prisonId).isEqualTo("WWI")
        assertThat(accounts[2].accountCode).isEqualTo(2101)
        assertThat(accounts[2].balance).isEqualTo(BigDecimal.ZERO)
        assertThat(accounts[2].holdBalance).isNull()
        assertThat(accounts[2].lastTransactionId).isEqualTo(55555)
        assertThat(accounts[2].transactionDate).isEqualTo(LocalDateTime.parse("2026-02-01T14:10:45"))

        assertThat(accounts[3].prisonId).isEqualTo("WWI")
        assertThat(accounts[3].accountCode).isEqualTo(2102)
        assertThat(accounts[3].balance).isEqualTo("-1.5")
        assertThat(accounts[3].holdBalance).isEqualTo(BigDecimal.ZERO)
        assertThat(accounts[3].lastTransactionId).isEqualTo(66666)
        assertThat(accounts[3].transactionDate).isEqualTo(LocalDateTime.parse("2026-02-02T09:20:01"))
      }
    }

    @Test
    fun getMigrationPrisonerBalance() {
      val balance = webTestClient.get().uri("/finance/prisoners/rootOffenderId/$id2/balance")
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
        assertThat(accounts[0].transactionDate).isEqualTo(LocalDateTime.parse("2026-04-02T16:11:12"))
        assertThat(accounts[1].prisonId).isEqualTo("LEI")
        assertThat(accounts[1].accountCode).isEqualTo(2102)
        assertThat(accounts[1].holdBalance).isEqualTo(BigDecimal(0))
        assertThat(accounts[1].lastTransactionId).isEqualTo(34567)
        assertThat(accounts[1].transactionDate).isEqualTo(LocalDateTime.parse("2026-02-02T10:00:11"))
        assertThat(accounts[2].prisonId).isEqualTo("LEI")
        assertThat(accounts[2].accountCode).isEqualTo(2103)
        assertThat(accounts[2].balance).isEqualTo("21.25")
        assertThat(accounts[2].holdBalance).isEqualTo("2.5")
        assertThat(accounts[2].lastTransactionId).isEqualTo(56789)
        assertThat(accounts[2].transactionDate).isEqualTo(LocalDateTime.parse("2026-02-02T04:12:12"))
      }
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/finance/prisoners/rootOffenderId/99999/balance")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/rootOffenderId/{rootOffenderId}/balance/reconcile")
  inner class PrisonerBalanceReconcileByRootOffenderIdTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/rootOffenderId/12345/balance/reconcile")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/rootOffenderId/12345/balance/reconcile")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/rootOffenderId/12345/balance/reconcile")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerBalanceWithDifferentPrisons() {
      val balance = webTestClient.get().uri("/finance/prisoners/rootOffenderId/$id1/balance/reconcile")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerAggregatedAccountsDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id1)
        assertThat(prisonNumber).isEqualTo("A1234BC")
        assertThat(accounts.size).isEqualTo(3)
        assertThat(accounts[0].accountCode).isEqualTo(2101)
        assertThat(accounts[0].balance).isEqualTo(BigDecimal.ZERO)
        assertThat(accounts[1].accountCode).isEqualTo(2102)
        // 11.25 - 1.5
        assertThat(accounts[1].balance).isEqualTo(BigDecimal(9.75))
        assertThat(accounts[2].accountCode).isEqualTo(2103)
        assertThat(accounts[2].balance).isEqualTo(BigDecimal(1.25))
      }
    }

    @Test
    fun getPrisonerBalance() {
      val balance = webTestClient.get().uri("/finance/prisoners/rootOffenderId/$id2/balance/reconcile")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerAggregatedAccountsDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id2)
        assertThat(prisonNumber).isEqualTo("B2345CD")
        assertThat(accounts.size).isEqualTo(3)
        assertThat(accounts[0].accountCode).isEqualTo(2101)
        assertThat(accounts[0].balance).isEqualTo(BigDecimal(0))
        assertThat(accounts[1].accountCode).isEqualTo(2102)
        assertThat(accounts[2].accountCode).isEqualTo(2103)
        assertThat(accounts[2].balance).isEqualTo("21.25")
      }
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/finance/prisoners/rootOffenderId/99999/balance/reconcile")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/{prisonNumber}/balance/reconcile")
  inner class PrisonerBalanceReconcileByPrisonNumberTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/B2345CD/balance/reconcile")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/B2345CD/balance/reconcile")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/B2345CD/balance/reconcile")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerBalanceWithDifferentPrisons() {
      val balance = webTestClient.get().uri("/finance/prisoners/A1234BC/balance/reconcile")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerAggregatedAccountsDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id1)
        assertThat(prisonNumber).isEqualTo("A1234BC")
        assertThat(accounts.size).isEqualTo(3)
        assertThat(accounts[0].accountCode).isEqualTo(2101)
        assertThat(accounts[0].balance).isEqualTo(BigDecimal.ZERO)
        assertThat(accounts[1].accountCode).isEqualTo(2102)
        // 11.25 - 1.5
        assertThat(accounts[1].balance).isEqualTo(BigDecimal(9.75))
        assertThat(accounts[2].accountCode).isEqualTo(2103)
        assertThat(accounts[2].balance).isEqualTo(BigDecimal(1.25))
      }
    }

    @Test
    fun getPrisonerBalance() {
      val balance = webTestClient.get().uri("/finance/prisoners/B2345CD/balance/reconcile")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerAggregatedAccountsDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id2)
        assertThat(prisonNumber).isEqualTo("B2345CD")
        assertThat(accounts.size).isEqualTo(3)
        assertThat(accounts[0].accountCode).isEqualTo(2101)
        assertThat(accounts[0].balance).isEqualTo(BigDecimal(0))
        assertThat(accounts[1].accountCode).isEqualTo(2102)

        assertThat(accounts[2].accountCode).isEqualTo(2103)
        assertThat(accounts[2].balance).isEqualTo("21.25")
      }
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/finance/prisoners/A999BC/balance/reconcile")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/{prisonerId}/balance/summary")
  inner class PrisonerBalanceSummaryTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/12345/balance/summary")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/12345/balance/summary")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/12345/balance/summary")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerBalanceSummaryWithDifferentPrisons() {
      val balance = webTestClient.get().uri("/finance/prisoners/$id1/balance/summary")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerBalanceSummaryDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id1)
        assertThat(prisonNumber).isEqualTo("A1234BC")
        assertThat(accounts.size).isEqualTo(3)
        assertThat(accounts[0].prisonId).isEqualTo("LEI")
        assertThat(accounts[0].accountCode).isEqualTo(2102)
        assertThat(accounts[0].balance).isEqualTo(BigDecimal(11.25))

        assertThat(accounts[1].prisonId).isEqualTo("LEI")
        assertThat(accounts[1].accountCode).isEqualTo(2103)
        assertThat(accounts[1].balance).isEqualTo(BigDecimal(1.25))

        assertThat(accounts[2].prisonId).isEqualTo("WWI")
        assertThat(accounts[2].accountCode).isEqualTo(2102)
        assertThat(accounts[2].balance).isEqualTo(BigDecimal(-1.5))
      }
    }

    @Test
    fun getPrisonerBalanceSummary() {
      val balance = webTestClient.get().uri("/finance/prisoners/$id2/balance/summary")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyResponse<PrisonerBalanceSummaryDto>()

      with(balance) {
        assertThat(rootOffenderId).isEqualTo(id2)
        assertThat(prisonNumber).isEqualTo("B2345CD")
        assertThat(accounts.size).isEqualTo(2)

        assertThat(accounts[0].prisonId).isEqualTo("LEI")
        assertThat(accounts[0].accountCode).isEqualTo(2102)
        assertThat(accounts[0].balance).isEqualTo(BigDecimal(12.25))

        assertThat(accounts[1].prisonId).isEqualTo("LEI")
        assertThat(accounts[1].accountCode).isEqualTo(2103)
        assertThat(accounts[1].balance).isEqualTo("21.25")
      }
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/finance/prisoners/99999/balance/summary")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
    }
  }
}
