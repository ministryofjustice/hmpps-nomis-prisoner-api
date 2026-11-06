package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.advances.AdvanceStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.advances.PrisonerAdvanceDto
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SubAccountType
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@WithMockAuthUser
@TestInstance(PER_CLASS)
class AdvancesResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var advance1: OffenderAdvance
  private lateinit var advance2: OffenderAdvance
  private lateinit var advance3: OffenderAdvance
  private lateinit var advance4: OffenderAdvance
  private lateinit var advance12: OffenderAdvance
  private var offender1Id: Long = 0
  private var offender2Id: Long = 0
  private var offender3Id: Long = 0
  val advance1Date: LocalDate = LocalDate.parse("2023-01-01")
  val advance2Date: LocalDate = LocalDate.parse("2023-02-02")
  val advance3Date: LocalDate = LocalDate.parse("2023-03-03")
  val advance4Date: LocalDate = LocalDate.parse("2023-04-04")

  @BeforeAll
  fun setUp() {
    nomisDataBuilder.build {
      offender1Id = offender {
        trustAccount()
        advance1 = advance(informationNumber = "10002000-10", deductionPriority = 1, advanceDate = advance1Date)
        // Multiple advances with same amount and date - this tests to ensure we don't return duplicate
        val advance11 = advance(informationNumber = "10002000-11", deductionPriority = 2, advanceDate = advance1Date)
        // Partial write off
        advance12 = advance(
          informationNumber = "10002000-12",
          deductionPriority = 3,
          advanceDate = advance1Date,
          writeOffAdjustmentAmount = BigDecimal.TWO,
          deductionAmount = BigDecimal.ZERO,
        )

        booking {
          transaction(transactionId = 21, subAccountType = SubAccountType.SPND, transactionType = "ADV", entryAmount = BigDecimal.TEN, entryDate = advance1Date) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2102, deductionId = advance1.deduction!!.deductionId, entryTime = LocalTime.parse("09:15:30"))
          }
          transaction(transactionId = 25, subAccountType = SubAccountType.SPND, transactionType = "ADV", entryAmount = BigDecimal.TEN, entryDate = advance1Date) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2102, deductionId = advance11.deduction!!.deductionId)
          }
          transaction(transactionId = 26, subAccountType = SubAccountType.SPND, transactionType = "ADV", entryAmount = BigDecimal.TEN, entryDate = advance1Date) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2102, deductionId = advance12.deduction!!.deductionId)
          }
        }
      }.id
      offender2Id = offender(nomsId = "A1234BC").id
      offender3Id = offender(nomsId = "A6789CD") {
        trustAccount()
        advance2 = advance(informationNumber = "10002000-21", deductionPriority = 1, advanceDate = advance2Date)
        booking {
          transaction(transactionId = 22, subAccountType = SubAccountType.SPND, transactionType = "ADV", entryAmount = BigDecimal.TEN, entryDate = advance2Date) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2102, deductionId = advance2.deduction!!.deductionId, entryTime = LocalTime.parse("10:15:30"))
          }
        }
        // paid off
        advance3 = advance(advanceAmount = BigDecimal.TWO, paymentAmount = BigDecimal.ONE, deductionAmount = BigDecimal.TWO, informationNumber = "10002000-31", deductionPriority = 2, advanceDate = advance3Date)
        booking {
          transaction(transactionId = 23, subAccountType = SubAccountType.SPND, transactionType = "ADV", entryAmount = BigDecimal.TWO, entryDate = advance3Date) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2102, deductionId = advance3.deduction!!.deductionId, entryTime = LocalTime.parse("10:15:30"))
          }
        }
        // written off
        advance4 = advance(advanceAmount = BigDecimal.TWO, writeOffAdjustmentAmount = BigDecimal.TWO, deductionAmount = BigDecimal.ZERO, informationNumber = "10002000-41", deductionPriority = 3, advanceDate = advance4Date)
        booking {
          transaction(transactionId = 24, subAccountType = SubAccountType.SPND, transactionType = "ADV", entryAmount = BigDecimal.TWO, entryDate = advance4Date) {
            generalLedgerTransaction(generalLedgerEntrySequence = 1, accountCode = 2102, deductionId = advance4.deduction!!.deductionId, entryTime = LocalTime.parse("11:15:30"))
          }
        }
        scheduledPayment()
      }.id
    }
  }

  @AfterAll
  fun tearDown() {
    repository.deleteAllTransactions()
    deleteOffenders()
  }

  @Nested
  @DisplayName("GET /finance/prisoners/{prisonNumber}/advances")
  inner class PrisonerAdvancesByPrisonNumberTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/A5194DY/advances")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/A5194DY/advances")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/A5194DY/advances")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerAdvances() {
      val advances = webTestClient.get().uri("/finance/prisoners/A5194DY/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      assertThat(advances).hasSize(3)
      advances.first().assertAdvance1()
    }

    @Test
    fun getPrisonerAdvancesEmptyList() {
      webTestClient.get().uri("/finance/prisoners/A1234BC/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("[]")
    }

    @Test
    fun getPrisonerAdvancesMultiple() {
      val advances = webTestClient.get().uri("/finance/prisoners/A6789CD/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      assertThat(advances).hasSize(3)
      advances[0].assertAdvance2()
      advances[1].assertAdvance3()
      advances[2].assertAdvance4()
    }

    @Test
    fun getActivePrisonerAdvances() {
      val advances = webTestClient.get().uri("/finance/prisoners/A6789CD/advances?activeOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      assertThat(advances).hasSize(1)
      advances.single().assertAdvance2()
    }

    @Test
    fun getActivePrisonerAdvancesIncludesPartialWriteOff() {
      val advances = webTestClient.get().uri("/finance/prisoners/A5194DY/advances?activeOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      assertThat(advances).hasSize(3)
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/root-offender-id/{rootOffenderId}/advances")
  inner class PrisonerAdvancesByRootOffenderIdTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender1Id/advances")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender1Id/advances")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender1Id/advances")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerAdvances() {
      val advances = webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender1Id/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      assertThat(advances).hasSize(3)
      advances.first().assertAdvance1()
    }

    @Test
    fun getPrisonerAdvancesEmptyList() {
      webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender2Id/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .json("[]")
    }

    @Test
    fun getPrisonerAdvancesMultiple() {
      val advances = webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender3Id/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      assertThat(advances).hasSize(3)
      advances[0].assertAdvance2()
      advances[1].assertAdvance3()
      advances[2].assertAdvance4()
    }

    @Test
    fun getActivePrisonerAdvances() {
      val advances = webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender3Id/advances?activeOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      assertThat(advances).hasSize(1)
      advances.single().assertAdvance2()
    }

    @Test
    fun getActivePrisonerAdvancesIncludesPartialWriteOff() {
      val advances = webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender1Id/advances?activeOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      assertThat(advances).hasSize(3)
    }
  }

  @Nested
  @DisplayName("GET /finance/advances/{advanceId}")
  inner class GetAdvanceTests {
    val advanceId = advance1.id

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/advances/$advanceId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/advances/$advanceId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/advances/$advanceId")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getActiveAdvance() {
      val advance = webTestClient.get().uri("/finance/advances/$advanceId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      advance.assertAdvance1()
    }

    @Test
    fun getPaidOffAdvance() {
      val advance = webTestClient.get().uri("/finance/advances/${advance3.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      advance.assertAdvance3()
    }

    @Test
    fun getWrittenOffAdvance() {
      val advance = webTestClient.get().uri("/finance/advances/${advance4.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      advance.assertAdvance4()
    }

    @Test
    fun getPartialWriteOffAdvance() {
      val advance = webTestClient.get().uri("/finance/advances/${advance12.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      assertThat(advance.status).isEqualTo(AdvanceStatus.ACTIVE)
    }

    @Test
    fun getAdvanceDoesNotExist() {
      webTestClient.get().uri("/finance/advances/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Offender advance with id 99999 not found")
        }
    }
  }

  @Nested
  @DisplayName("GET /finance/advances/deduction-id/{deductionId}")
  inner class GetAdvanceByDeductionIdTests {
    val deductionId = advance1.deduction!!.deductionId

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/advances/deduction-id/$deductionId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/advances/deduction-id/$deductionId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/advances/deduction-id/$deductionId")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getActiveAdvance() {
      val advance = webTestClient.get().uri("/finance/advances/deduction-id/$deductionId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      advance.assertAdvance1()
    }

    @Test
    fun getPaidOffAdvance() {
      val advance = webTestClient.get().uri("/finance/advances/deduction-id/${advance3.deduction!!.deductionId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      advance.assertAdvance3()
    }

    @Test
    fun getWrittenOffAdvance() {
      val advance = webTestClient.get().uri("/finance/advances/deduction-id/${advance4.deduction!!.deductionId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      advance.assertAdvance4()
    }

    @Test
    fun getPartialWriteOffAdvance() {
      val advance = webTestClient.get().uri("/finance/advances/deduction-id/${advance12.deduction!!.deductionId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PrisonerAdvanceDto>()
        .returnResult()
        .responseBody!!

      assertThat(advance.status).isEqualTo(AdvanceStatus.ACTIVE)
    }

    @Test
    fun getAdvanceDoesNotExist() {
      webTestClient.get().uri("/finance/advances/deduction-id/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Offender advance with deduction id 99999 not found")
        }
    }
  }

  @Nested
  @DisplayName("GET /finance/advances/active-count")
  inner class GetActiveAdvancesCountTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/advances/active-count")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/advances/active-count")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/advances/active-count")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getActiveAdvancesCount() {
      webTestClient.get().uri("/finance/advances/active-count")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("activeCount").isEqualTo(4)
    }
  }

  private fun PrisonerAdvanceDto.assertAdvance1() {
    assertThat(id).isEqualTo(advance1.id)
    assertThat(prisonNumber).isEqualTo("A5194DY")
    assertThat(caseloadId).isEqualTo("MDI")
    assertThat(advanceAmount).isEqualTo(1000L)
    assertThat(advanceDate).isEqualTo(advance1Date)
    assertThat(repaymentAmount).isEqualTo(100L)
    assertThat(startDate).isEqualTo(LocalDate.now())
    assertThat(informationNumber).isEqualTo("10002000-10")
    assertThat(advanceCreateTransactionId).isEqualTo(21L)
    assertThat(lastTransactionTimestamp).isEqualTo(advance1Date.atTime(9, 15, 30))
    assertThat(outstandingBalance).isEqualTo(1000L)
    assertThat(status).isEqualTo(AdvanceStatus.ACTIVE)
    assertThat(deductionAmount).isEqualTo(0L)
    assertThat(adjustmentAmount).isNull()
    assertThat(createdBy).isEqualTo("SA")
    assertThat(createDatetime).isEqualTo(advance1.createDatetime)
  }

  private fun PrisonerAdvanceDto.assertAdvance2() {
    assertThat(id).isEqualTo(advance2.id)
    assertThat(prisonNumber).isEqualTo("A6789CD")
    assertThat(caseloadId).isEqualTo("MDI")
    assertThat(advanceAmount).isEqualTo(1000L)
    assertThat(advanceDate).isEqualTo(advance2Date)
    assertThat(repaymentAmount).isEqualTo(100L)
    assertThat(startDate).isEqualTo(LocalDate.now())
    assertThat(informationNumber).isEqualTo("10002000-21")
    assertThat(advanceCreateTransactionId).isEqualTo(22L)
    assertThat(lastTransactionTimestamp).isEqualTo(advance2Date.atTime(10, 15, 30))
    assertThat(outstandingBalance).isEqualTo(1000L)
    assertThat(status).isEqualTo(AdvanceStatus.ACTIVE)
    assertThat(deductionAmount).isEqualTo(0L)
    assertThat(adjustmentAmount).isNull()
    assertThat(createdBy).isEqualTo("SA")
    assertThat(createDatetime).isEqualTo(advance2.createDatetime)
  }

  private fun PrisonerAdvanceDto.assertAdvance3() {
    assertThat(id).isEqualTo(advance3.id)
    assertThat(prisonNumber).isEqualTo("A6789CD")
    assertThat(caseloadId).isEqualTo("MDI")
    assertThat(advanceAmount).isEqualTo(200L)
    assertThat(advanceDate).isEqualTo(advance3Date)
    assertThat(repaymentAmount).isEqualTo(100L)
    assertThat(startDate).isEqualTo(LocalDate.now())
    assertThat(informationNumber).isEqualTo("10002000-31")
    assertThat(advanceCreateTransactionId).isEqualTo(23L)
    assertThat(lastTransactionTimestamp).isEqualTo(advance3Date.atTime(10, 15, 30))
    assertThat(outstandingBalance).isEqualTo(0L)
    assertThat(status).isEqualTo(AdvanceStatus.REPAID)
    assertThat(deductionAmount).isEqualTo(200L)
    assertThat(adjustmentAmount).isNull()
    assertThat(createdBy).isEqualTo("SA")
    assertThat(createDatetime).isEqualTo(advance3.createDatetime)
  }

  private fun PrisonerAdvanceDto.assertAdvance4() {
    assertThat(id).isEqualTo(advance4.id)
    assertThat(prisonNumber).isEqualTo("A6789CD")
    assertThat(caseloadId).isEqualTo("MDI")
    assertThat(advanceAmount).isEqualTo(200L)
    assertThat(advanceDate).isEqualTo(advance4Date)
    assertThat(repaymentAmount).isEqualTo(100L)
    assertThat(startDate).isEqualTo(LocalDate.now())
    assertThat(informationNumber).isEqualTo("10002000-41")
    assertThat(advanceCreateTransactionId).isEqualTo(24L)
    assertThat(lastTransactionTimestamp).isEqualTo(advance4Date.atTime(11, 15, 30))
    assertThat(outstandingBalance).isEqualTo(0L)
    assertThat(status).isEqualTo(AdvanceStatus.WRITTEN_OFF)
    assertThat(deductionAmount).isEqualTo(0L)
    assertThat(adjustmentAmount).isEqualTo(200L)
    assertThat(createdBy).isEqualTo("SA")
    assertThat(createDatetime).isEqualTo(advance4.createDatetime)
  }
}
