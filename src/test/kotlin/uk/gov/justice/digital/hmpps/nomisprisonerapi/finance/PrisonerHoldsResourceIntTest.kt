package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.GeneralLedgerTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransaction
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SubAccountType
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate
import java.time.LocalDateTime

@WithMockAuthUser
class PrisonerHoldsResourceIntTest : IntegrationTestBase() {

  private lateinit var offender: Offender
  private lateinit var offender2: Offender
  private lateinit var offender4: Offender
  private lateinit var transaction1: OffenderTransaction
  private lateinit var transaction2: OffenderTransaction
  private lateinit var transaction3: OffenderTransaction
  private lateinit var glTransaction1: GeneralLedgerTransaction
  private lateinit var glTransaction2: GeneralLedgerTransaction
  private lateinit var glTransaction3: GeneralLedgerTransaction

  @BeforeEach
  fun setUp() {
    nomisDataBuilder.build {
      offender = offender {
        booking {
          transaction1 = transaction(transactionId = 2, subAccountType = SubAccountType.SAV, transactionType = "HOA", holdNumber = 1, clientUniqueRef = "Test-0123") {
            glTransaction1 = generalLedgerTransaction(1, 2000)
            glTransaction2 = generalLedgerTransaction(2, 2100)
          }
          transaction2 = transaction(transactionId = 3, subAccountType = SubAccountType.REG, transactionType = "SPEN", entryDate = LocalDate.parse("2025-08-11")) {
            glTransaction3 = generalLedgerTransaction(1, 2101)
          }
          transaction(transactionId = 3, subAccountType = SubAccountType.REG, transactionEntrySequence = 2, transactionType = "DPST")
          transaction3 = transaction(transactionId = 4, subAccountType = SubAccountType.REG, transactionEntrySequence = 1, transactionType = "DPST")
        }
      }
      offender2 = offender(nomsId = "A1234BC") {
        booking {
          transaction(transactionId = 8, subAccountType = SubAccountType.SAV, transactionType = "HOA", holdNumber = 2, clientUniqueRef = "Test-1234") {
            glTransaction1 = generalLedgerTransaction(1, 2000)
            glTransaction2 = generalLedgerTransaction(2, 2100)
          }
          transaction(transactionId = 6, subAccountType = SubAccountType.REG, transactionType = "SPEN", entryDate = LocalDate.parse("2025-08-11")) {
            glTransaction3 = generalLedgerTransaction(1, 2101)
          }
          transaction(transactionId = 6, subAccountType = SubAccountType.REG, transactionEntrySequence = 2, transactionType = "DPST")
          transaction(transactionId = 7, subAccountType = SubAccountType.REG, transactionEntrySequence = 1, transactionType = "DPST")
        }
      }
      offender(nomsId = "A5678CD") {
        booking {
          transaction(transactionId = 8, subAccountType = SubAccountType.SAV, transactionType = "HOA", holdNumber = 3, holdClearFlag = true, clientUniqueRef = "Test-5678") {
            glTransaction1 = generalLedgerTransaction(1, 2000)
            glTransaction2 = generalLedgerTransaction(2, 2100)
          }
          transaction(transactionId = 9, subAccountType = SubAccountType.REG, transactionType = "SPEN", entryDate = LocalDate.parse("2025-08-11")) {
            glTransaction3 = generalLedgerTransaction(1, 2101)
          }
          transaction(transactionId = 9, subAccountType = SubAccountType.REG, transactionEntrySequence = 2, transactionType = "DPST")
          transaction(transactionId = 10, subAccountType = SubAccountType.REG, transactionEntrySequence = 1, transactionType = "HOR")
        }
      }
      offender4 = offender(nomsId = "A6789CD") {
        booking {
          transaction(transactionId = 8, subAccountType = SubAccountType.SAV, transactionType = "HOA", holdNumber = 4, holdClearFlag = true, clientUniqueRef = "Test-6789") {
            glTransaction1 = generalLedgerTransaction(1, 2000)
            glTransaction2 = generalLedgerTransaction(2, 2100)
          }
          transaction(transactionId = 9, subAccountType = SubAccountType.REG, transactionType = "HOA", holdNumber = 5, holdClearFlag = false, clientUniqueRef = "Test-7890") {
            glTransaction3 = generalLedgerTransaction(1, 2101)
          }
          transaction(transactionId = 9, subAccountType = SubAccountType.REG, transactionEntrySequence = 2, transactionType = "HOA", holdNumber = 6, holdClearFlag = false)
          transaction(transactionId = 10, subAccountType = SubAccountType.REG, transactionEntrySequence = 1, transactionType = "HOR")
        }
      }
    }
  }

  @AfterEach
  fun tearDown() {
    repository.deleteAllTransactions()
    repository.deleteOffenders()
  }

  @Nested
  @DisplayName("GET /finance/prisoners/holds/{prisonNumber}")
  inner class PrisonerHoldByPrisonNumberTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/holds/A5194DY")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/holds/A5194DY")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/holds/A5194DY")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerHolds() {
      webTestClient.get().uri("/finance/prisoners/holds/A5194DY")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].txnId").isEqualTo(2)
        .jsonPath("[0].txnEntrySeq").isEqualTo(1)
        .jsonPath("[0].txnEntryDesc").isEqualTo("entryDescription")
        .jsonPath("[0].txnEntryDate").isEqualTo(LocalDateTime.parse("2025-06-01T00:00:00"))
        .jsonPath("[0].txnReferenceNumber").isEqualTo("FG1/12")
        .jsonPath("[0].txnEntryAmount").isEqualTo(2.34)
        .jsonPath("[0].holdNumber").isEqualTo(1)
        .jsonPath("[0].clientUniqueRef").isEqualTo("Test-0123")
    }

    @Test
    fun getPrisonerHoldsEmptyList() {
      webTestClient.get().uri("/finance/prisoners/holds/A1234BC")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("[]")
    }

    @Test
    fun getPrisonerHoldsMultiple() {
      webTestClient.get().uri("/finance/prisoners/holds/A6789CD")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(2)
        .jsonPath("[0].holdNumber").isEqualTo(5)
        .jsonPath("[1].holdNumber").isEqualTo(6)
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/holds/rootOffenderId/{rootOffenderId}")
  inner class PrisonerHoldByRootOffenderIdTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/holds/rootOffenderId/${offender.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/holds/rootOffenderId/${offender.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/holds/rootOffenderId/${offender.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerHolds() {
      webTestClient.get().uri("/finance/prisoners/holds/rootOffenderId/${offender.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].txnId").isEqualTo(2)
        .jsonPath("[0].txnEntrySeq").isEqualTo(1)
        .jsonPath("[0].txnEntryDesc").isEqualTo("entryDescription")
        .jsonPath("[0].txnEntryDate").isEqualTo(LocalDateTime.parse("2025-06-01T00:00:00"))
        .jsonPath("[0].txnReferenceNumber").isEqualTo("FG1/12")
        .jsonPath("[0].txnEntryAmount").isEqualTo(2.34)
        .jsonPath("[0].holdNumber").isEqualTo(1)
        .jsonPath("[0].clientUniqueRef").isEqualTo("Test-0123")
    }

    @Test
    fun getPrisonerHoldsEmptyList() {
      webTestClient.get().uri("/finance/prisoners/holds/rootOffenderId/${offender2.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("[]")
    }

    @Test
    fun getPrisonerHoldsMultiple() {
      webTestClient.get().uri("/finance/prisoners/holds/rootOffenderId/${offender4.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(2)
        .jsonPath("[0].holdNumber").isEqualTo(5)
        .jsonPath("[1].holdNumber").isEqualTo(6)
    }
  }
}
