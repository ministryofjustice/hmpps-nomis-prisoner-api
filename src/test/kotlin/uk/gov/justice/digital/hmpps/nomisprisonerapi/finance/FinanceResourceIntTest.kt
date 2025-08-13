package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate

@WithMockAuthUser
class FinanceResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var repository: Repository

  private lateinit var offender: Offender
  private lateinit var transaction1: OffenderTransaction
  private lateinit var transaction2: OffenderTransaction
  private lateinit var glTransaction1: GeneralLedgerTransaction
  private lateinit var glTransaction2: GeneralLedgerTransaction
  private lateinit var glTransaction3: GeneralLedgerTransaction

  @BeforeEach
  fun setUp() {
    nomisDataBuilder.build {
      offender = offender {
        booking {
          transaction1 = transaction("DPST") {
            glTransaction1 = generalLedgerTransaction(1, 2000)
            glTransaction2 = generalLedgerTransaction(2, 2100)
          }
          transaction2 = transaction("SPEN", LocalDate.parse("2025-08-11")) {
            glTransaction3 = generalLedgerTransaction(1, 2101)
          }
        }
      }
    }
  }

  @AfterEach
  fun tearDown() {
    repository.deleteAllTransactions()
    repository.delete(offender)
  }

  @Nested
  @DisplayName("GET /transactions/{transactionId}")
  inner class OffenderTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/transactions/99")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/transactions/99")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/transactions/99")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getTransaction() {
      webTestClient.get().uri("/transactions/${transaction1.transactionId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$[0].transactionId").isEqualTo(transaction1.transactionId)
        .jsonPath("$[0].transactionEntrySequence").isEqualTo(transaction1.transactionEntrySequence)
        .jsonPath("$[0].offenderId").isEqualTo(transaction1.offenderBooking!!.offender.id)
        .jsonPath("$[0].offenderNo").isEqualTo(transaction1.offenderBooking!!.offender.nomsId)
        .jsonPath("$[0].bookingId").isEqualTo(transaction1.offenderBooking!!.bookingId)
        .jsonPath("$[0].caseloadId").isEqualTo(transaction1.trustAccount.id.caseloadId)
        .jsonPath("$[0].amount").value<Double> {
          assertThat(it).isCloseTo(transaction1.entryAmount.toDouble(), Percentage.withPercentage(0.1))
        }
        .jsonPath("$[0].type").isEqualTo(transaction1.transactionType.type)
        .jsonPath("$[0].postingType").isEqualTo(transaction1.postingType.name)
        .jsonPath("$[0].description").isEqualTo(transaction1.entryDescription!!)
        .jsonPath("$[0].entryDate").isEqualTo(transaction1.entryDate)
        .jsonPath("$[0].clientReference").isEqualTo(transaction1.clientUniqueRef!!)
        .jsonPath("$[0].reference").isEqualTo(transaction1.transactionReferenceNumber!!)
        .jsonPath("$[0].subAccountType").isEqualTo(transaction1.subAccountType.name)
        .jsonPath("$[0].generalLedgerTransactions[0].transactionId").isEqualTo(glTransaction1.transactionId)
        .jsonPath("$[0].generalLedgerTransactions[0].transactionEntrySequence")
        .isEqualTo(glTransaction1.transactionEntrySequence)
        .jsonPath("$[0].generalLedgerTransactions[0].accountCode").isEqualTo(glTransaction1.accountCode.accountCode)
        .jsonPath("$[0].generalLedgerTransactions[1].transactionId").isEqualTo(glTransaction2.transactionId)
        .jsonPath("$[0].generalLedgerTransactions[1].transactionEntrySequence")
        .isEqualTo(glTransaction2.transactionEntrySequence)
        .jsonPath("$[0].generalLedgerTransactions[1].accountCode").isEqualTo(glTransaction2.accountCode.accountCode)
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/transactions/9999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("[]")
    }
  }

  @Nested
  @DisplayName("GET /transactions/{transactionId}/general-ledger")
  inner class GL {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/transactions/99/general-ledger")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/transactions/99/general-ledger")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/transactions/99/general-ledger")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getGLTransaction() {
      webTestClient.get().uri("/transactions/${transaction1.transactionId}/general-ledger")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("$[0].transactionId").isEqualTo(glTransaction1.transactionId)
        .jsonPath("$[0].transactionEntrySequence").isEqualTo(glTransaction1.transactionEntrySequence)
        .jsonPath("$[0].generalLedgerEntrySequence").isEqualTo(glTransaction1.generalLedgerEntrySequence)
        .jsonPath("$[0].caseloadId").isEqualTo(glTransaction1.caseloadId)
        .jsonPath("$[0].amount").value<Double> {
          assertThat(it).isCloseTo(glTransaction1.entryAmount.toDouble(), Percentage.withPercentage(0.1))
        }
        .jsonPath("$[0].type").isEqualTo(glTransaction1.transactionType.type)
        .jsonPath("$[0].postingType").isEqualTo(glTransaction1.postUsage.name)
        .jsonPath("$[0].accountCode").isEqualTo(glTransaction1.accountCode.accountCode)
        .jsonPath("$[0].description").isEqualTo(glTransaction1.entryDescription!!)
        .jsonPath("$[0].transactionTimestamp").isEqualTo(glTransaction1.entryDate.atTime(glTransaction2.entryTime))
        .jsonPath("$[0].reference").isEqualTo(glTransaction1.transactionReferenceNumber!!)
        .jsonPath("$[1].transactionId").isEqualTo(glTransaction2.transactionId)
        .jsonPath("$[1].transactionEntrySequence").isEqualTo(glTransaction2.transactionEntrySequence)
        .jsonPath("$[1].generalLedgerEntrySequence").isEqualTo(glTransaction2.generalLedgerEntrySequence)
        .jsonPath("$[1].caseloadId").isEqualTo(glTransaction2.caseloadId)
        .jsonPath("$[1].amount").value<Double> {
          assertThat(it).isCloseTo(glTransaction2.entryAmount.toDouble(), Percentage.withPercentage(0.1))
        }
        .jsonPath("$[1].type").isEqualTo(glTransaction2.transactionType.type)
        .jsonPath("$[1].postingType").isEqualTo(glTransaction2.postUsage.name)
        .jsonPath("$[1].accountCode").isEqualTo(glTransaction2.accountCode.accountCode)
        .jsonPath("$[1].description").isEqualTo(glTransaction2.entryDescription!!)
        .jsonPath("$[1].transactionTimestamp").isEqualTo(glTransaction2.entryDate.atTime(glTransaction2.entryTime))
        .jsonPath("$[1].reference").isEqualTo(glTransaction2.transactionReferenceNumber!!)
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/transactions/9999/general-ledger")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("[]")
    }
  }

  @Nested
  @DisplayName("GET /transactions/{date}/first")
  inner class GetIdAtDate {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/transactions/2025-01-01/first")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/transactions/2025-01-01/first")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/transactions/2025-01-01/first")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getTransactionId() {
      val id = webTestClient.get().uri("/transactions/2025-08-11/first")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody(Long::class.java)
        .returnResult().responseBody!!
      assertThat(id).isEqualTo(transaction2.transactionId)
    }

    @Test
    fun `none found`() {
      webTestClient.get().uri("/transactions/2025-08-12/first")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("developerMessage").isEqualTo("No transactions found with date 2025-08-12")
    }
  }

  @Nested
  @DisplayName("GET /transactions/from/{transactionId}/all")
  inner class NonOffenderTransactionsFromId {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/transactions/from/99/1/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/transactions/from/99/1/1")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/transactions/from/99/1/1")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          generalLedgerTransaction(10, 1, 1)
          generalLedgerTransaction(10, 1, 2)
          generalLedgerTransaction(11, 1, 1)
          generalLedgerTransaction(11, 1, 2)
          generalLedgerTransaction(11, 2, 1)
          generalLedgerTransaction(11, 2, 2)
          generalLedgerTransaction(12, 1, 1)
        }
      }

      @Test
      fun getAll() {
        webTestClient.get().uri("/transactions/from/9/99/99")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$.length()").isEqualTo(7)
      }

      @Test
      fun `get transactions limited by pagesize`() {
        webTestClient.get().uri("/transactions/from/9/99/99?pageSize=2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].transactionId").isEqualTo(10)
          .jsonPath("$[0].transactionEntrySequence").isEqualTo(1)
          .jsonPath("$[0].generalLedgerEntrySequence").isEqualTo(1)
          .jsonPath("$[1].transactionId").isEqualTo(10)
          .jsonPath("$[1].transactionEntrySequence").isEqualTo(1)
          .jsonPath("$[1].generalLedgerEntrySequence").isEqualTo(2)
          .jsonPath("$.length()").isEqualTo(2)
      }

      @Test
      fun `get transactions starting midway through seq`() {
        webTestClient.get().uri("/transactions/from/11/1/2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].transactionId").isEqualTo(11)
          .jsonPath("$[0].transactionEntrySequence").isEqualTo(2)
          .jsonPath("$[0].generalLedgerEntrySequence").isEqualTo(1)
          .jsonPath("$[1].transactionId").isEqualTo(11)
          .jsonPath("$[1].transactionEntrySequence").isEqualTo(2)
          .jsonPath("$[1].generalLedgerEntrySequence").isEqualTo(2)
          .jsonPath("$[2].transactionId").isEqualTo(12)
          .jsonPath("$[2].transactionEntrySequence").isEqualTo(1)
          .jsonPath("$[2].generalLedgerEntrySequence").isEqualTo(1)
          .jsonPath("$.length()").isEqualTo(3)
      }

      @Test
      fun `get transactions starting midway through GL seq`() {
        webTestClient.get().uri("/transactions/from/11/2/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("$[0].transactionId").isEqualTo(11)
          .jsonPath("$[0].transactionEntrySequence").isEqualTo(2)
          .jsonPath("$[0].generalLedgerEntrySequence").isEqualTo(2)
          .jsonPath("$[1].transactionId").isEqualTo(12)
          .jsonPath("$[1].transactionEntrySequence").isEqualTo(1)
          .jsonPath("$[1].generalLedgerEntrySequence").isEqualTo(1)
          .jsonPath("$.length()").isEqualTo(2)
      }

      @Test
      fun `none found`() {
        webTestClient.get().uri("/transactions/from/20/1/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_TRANSACTIONS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .json("[]")
      }
    }
  }
}
