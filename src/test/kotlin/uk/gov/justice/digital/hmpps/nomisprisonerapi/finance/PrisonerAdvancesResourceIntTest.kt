package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAdvance
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SubAccountType
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit.SECONDS

@WithMockAuthUser
@TestInstance(PER_CLASS)
class PrisonerAdvancesResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  private lateinit var advance1: OffenderAdvance
  private lateinit var advance2: OffenderAdvance
  private lateinit var advance3: OffenderAdvance
  private lateinit var advance4: OffenderAdvance
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
        advance1 = advance(informationNumber = "10002000-1", deductionPriority = 1, advanceDate = advance1Date)
        booking {
          transaction(transactionId = 21, subAccountType = SubAccountType.SAV, transactionType = "ADV", entryAmount = BigDecimal.TEN, entryDate = advance1Date)
        }
      }.id
      offender2Id = offender(nomsId = "A1234BC").id
      offender3Id = offender(nomsId = "A6789CD") {
        trustAccount()
        advance2 = advance(informationNumber = "10002000-2", deductionPriority = 1, advanceDate = advance2Date)
        booking {
          transaction(transactionId = 22, subAccountType = SubAccountType.SAV, transactionType = "ADV", entryAmount = BigDecimal.TEN, entryDate = advance2Date)
        }
        // paid off
        advance3 = advance(advanceAmount = BigDecimal.TWO, paymentAmount = BigDecimal.ONE, deductionAmount = BigDecimal.TWO, informationNumber = "10002000-3", deductionPriority = 2, advanceDate = advance3Date)
        booking {
          transaction(transactionId = 23, subAccountType = SubAccountType.SAV, transactionType = "ADV", entryAmount = BigDecimal.TWO, entryDate = advance3Date)
        }
        // written off
        advance4 = advance(advanceAmount = BigDecimal.TWO, writeOffAdjustmentAmount = BigDecimal.TWO, deductionAmount = BigDecimal.ZERO, informationNumber = "10002000-4", deductionPriority = 3, advanceDate = advance4Date)
        booking {
          transaction(transactionId = 24, subAccountType = SubAccountType.SAV, transactionType = "ADV", entryAmount = BigDecimal.TWO, entryDate = advance4Date)
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
      webTestClient.get().uri("/finance/prisoners/A5194DY/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].id").isEqualTo(advance1.id)
        .jsonPath("[0].prisonNumber").isEqualTo("A5194DY")
        .jsonPath("[0].caseloadId").isEqualTo("MDI")
        .jsonPath("[0].advanceAmount").isEqualTo(1000)
        .jsonPath("[0].advanceDate").isEqualTo(advance1Date.toString())
        .jsonPath("[0].repaymentAmount").isEqualTo(100)
        .jsonPath("[0].startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].createdBy").isEqualTo("SA")
        .jsonPath("[0].createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("[0].informationNumber").isEqualTo("10002000-1")
        .jsonPath("[0].status").isEqualTo("ACTIVE")
        .jsonPath("[0].advanceTransactionId").isEqualTo(21)
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
      webTestClient.get().uri("/finance/prisoners/A6789CD/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(3)
        .jsonPath("[0].id").isEqualTo(advance2.id)
        .jsonPath("[0].advanceTransactionId").isEqualTo(22)
        .jsonPath("[0].status").isEqualTo("ACTIVE")
        .jsonPath("[1].id").isEqualTo(advance3.id)
        .jsonPath("[1].advanceTransactionId").isEqualTo(23)
        .jsonPath("[1].status").isEqualTo("REPAID")
        .jsonPath("[2].id").isEqualTo(advance4.id)
        .jsonPath("[2].advanceTransactionId").isEqualTo(24)
        .jsonPath("[2].status").isEqualTo("WRITTEN_OFF")
    }

    @Test
    fun getActivePrisonerAdvances() {
      webTestClient.get().uri("/finance/prisoners/A6789CD/advances?activeOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].id").isEqualTo(advance2.id)
        .jsonPath("[0].prisonNumber").isEqualTo("A6789CD")
        .jsonPath("[0].caseloadId").isEqualTo("MDI")
        .jsonPath("[0].advanceAmount").isEqualTo(1000)
        .jsonPath("[0].advanceDate").isEqualTo(advance2Date.toString())
        .jsonPath("[0].repaymentAmount").isEqualTo(100)
        .jsonPath("[0].startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].createdBy").isEqualTo("SA")
        .jsonPath("[0].createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("[0].informationNumber").isEqualTo("10002000-2")
        .jsonPath("[0].advanceTransactionId").isEqualTo(22)
        .jsonPath("[0].status").isEqualTo("ACTIVE")
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
      webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender1Id/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].id").isEqualTo(advance1.id)
        .jsonPath("[0].prisonNumber").isEqualTo("A5194DY")
        .jsonPath("[0].caseloadId").isEqualTo("MDI")
        .jsonPath("[0].advanceAmount").isEqualTo(1000)
        .jsonPath("[0].advanceDate").isEqualTo(advance1Date.toString())
        .jsonPath("[0].repaymentAmount").isEqualTo(100)
        .jsonPath("[0].startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].createdBy").isEqualTo("SA")
        .jsonPath("[0].createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("[0].informationNumber").isEqualTo("10002000-1")
        .jsonPath("[0].advanceTransactionId").isEqualTo(21)
        .jsonPath("[0].status").isEqualTo("ACTIVE")
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
      webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender3Id/advances")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(3)
        .jsonPath("[0].id").isEqualTo(advance2.id)
        .jsonPath("[0].advanceTransactionId").isEqualTo(22)
        .jsonPath("[0].status").isEqualTo("ACTIVE")
        .jsonPath("[1].id").isEqualTo(advance3.id)
        .jsonPath("[1].advanceTransactionId").isEqualTo(23)
        .jsonPath("[1].status").isEqualTo("REPAID")
        .jsonPath("[2].id").isEqualTo(advance4.id)
        .jsonPath("[2].advanceTransactionId").isEqualTo(24)
        .jsonPath("[2].status").isEqualTo("WRITTEN_OFF")
    }

    @Test
    fun getActivePrisonerAdvances() {
      webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender3Id/advances?activeOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("size()").isEqualTo(1)
        .jsonPath("[0].id").isEqualTo(advance2.id)
        .jsonPath("[0].prisonNumber").isEqualTo("A6789CD")
        .jsonPath("[0].caseloadId").isEqualTo("MDI")
        .jsonPath("[0].advanceAmount").isEqualTo(1000)
        .jsonPath("[0].advanceDate").isEqualTo(advance2Date.toString())
        .jsonPath("[0].repaymentAmount").isEqualTo(100)
        .jsonPath("[0].startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("[0].createdBy").isEqualTo("SA")
        .jsonPath("[0].createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("[0].informationNumber").isEqualTo("10002000-2")
        .jsonPath("[0].advanceTransactionId").isEqualTo(22)
        .jsonPath("[0].status").isEqualTo("ACTIVE")
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/advances/{advanceId}")
  inner class GetAdvanceTests {
    val advanceId = advance1.id

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/advances/$advanceId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/advances/$advanceId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/advances/$advanceId")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getActiveAdvance() {
      webTestClient.get().uri("/finance/prisoners/advances/$advanceId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("id").isEqualTo(advanceId)
        .jsonPath("prisonNumber").isEqualTo("A5194DY")
        .jsonPath("caseloadId").isEqualTo("MDI")
        .jsonPath("advanceAmount").isEqualTo(1000)
        .jsonPath("advanceDate").isEqualTo(advance1Date.toString())
        .jsonPath("repaymentAmount").isEqualTo(100)
        .jsonPath("startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("createdBy").isEqualTo("SA")
        .jsonPath("createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("informationNumber").isEqualTo("10002000-1")
        .jsonPath("advanceTransactionId").isEqualTo(21)
        .jsonPath("status").isEqualTo("ACTIVE")
    }

    @Test
    fun getPaidOffAdvance() {
      webTestClient.get().uri("/finance/prisoners/advances/${advance3.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("id").isEqualTo(advance3.id)
        .jsonPath("prisonNumber").isEqualTo("A6789CD")
        .jsonPath("caseloadId").isEqualTo("MDI")
        .jsonPath("advanceAmount").isEqualTo(200)
        .jsonPath("advanceDate").isEqualTo(advance3Date.toString())
        .jsonPath("repaymentAmount").isEqualTo(100)
        .jsonPath("startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("createdBy").isEqualTo("SA")
        .jsonPath("createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("informationNumber").isEqualTo("10002000-3")
        .jsonPath("advanceTransactionId").isEqualTo(23)
        .jsonPath("status").isEqualTo("REPAID")
        .jsonPath("deductionAmount").isEqualTo(200)
        .jsonPath("adjustmentReason").doesNotExist()
        .jsonPath("adjustmentAmount").doesNotExist()
    }

    @Test
    fun getWrittenOffAdvance() {
      webTestClient.get().uri("/finance/prisoners/advances/${advance4.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("id").isEqualTo(advance4.id)
        .jsonPath("prisonNumber").isEqualTo("A6789CD")
        .jsonPath("caseloadId").isEqualTo("MDI")
        .jsonPath("advanceAmount").isEqualTo(200)
        .jsonPath("advanceDate").isEqualTo(advance4Date.toString())
        .jsonPath("repaymentAmount").isEqualTo(100)
        .jsonPath("startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("createdBy").isEqualTo("SA")
        .jsonPath("createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("informationNumber").isEqualTo("10002000-4")
        .jsonPath("advanceTransactionId").isEqualTo(24)
        .jsonPath("status").isEqualTo("WRITTEN_OFF")
        .jsonPath("deductionAmount").isEqualTo(0)
        .jsonPath("adjustmentReason").isEqualTo("WRT")
        .jsonPath("adjustmentAmount").isEqualTo(200)
    }

    @Test
    fun getAdvanceDoesNotExist() {
      webTestClient.get().uri("/finance/prisoners/advances/99999")
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
  @DisplayName("GET /finance/prisoners/advances/deduction-id/{deductionId}")
  inner class GetAdvanceByDeductionIdTests {
    val advanceId = advance1.id
    val deductionId = advance1.deduction!!.deductionId

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/advances/deduction-id/$deductionId")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/advances/deduction-id/$deductionId")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/advances/deduction-id/$deductionId")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getActiveAdvance() {
      webTestClient.get().uri("/finance/prisoners/advances/deduction-id/$deductionId")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("id").isEqualTo(advanceId)
        .jsonPath("prisonNumber").isEqualTo("A5194DY")
        .jsonPath("caseloadId").isEqualTo("MDI")
        .jsonPath("advanceAmount").isEqualTo(1000)
        .jsonPath("advanceDate").isEqualTo(advance1Date.toString())
        .jsonPath("repaymentAmount").isEqualTo(100)
        .jsonPath("startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("createdBy").isEqualTo("SA")
        .jsonPath("createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("informationNumber").isEqualTo("10002000-1")
        .jsonPath("advanceTransactionId").isEqualTo(21)
        .jsonPath("status").isEqualTo("ACTIVE")
    }

    @Test
    fun getPaidOffAdvance() {
      webTestClient.get().uri("/finance/prisoners/advances/deduction-id/${advance3.deduction!!.deductionId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("id").isEqualTo(advance3.id)
        .jsonPath("prisonNumber").isEqualTo("A6789CD")
        .jsonPath("caseloadId").isEqualTo("MDI")
        .jsonPath("advanceAmount").isEqualTo(200)
        .jsonPath("advanceDate").isEqualTo(advance3Date.toString())
        .jsonPath("repaymentAmount").isEqualTo(100)
        .jsonPath("startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("createdBy").isEqualTo("SA")
        .jsonPath("createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("informationNumber").isEqualTo("10002000-3")
        .jsonPath("advanceTransactionId").isEqualTo(23)
        .jsonPath("status").isEqualTo("REPAID")
    }

    @Test
    fun getWrittenOffAdvance() {
      webTestClient.get().uri("/finance/prisoners/advances/deduction-id/${advance4.deduction!!.deductionId}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("id").isEqualTo(advance4.id)
        .jsonPath("prisonNumber").isEqualTo("A6789CD")
        .jsonPath("caseloadId").isEqualTo("MDI")
        .jsonPath("advanceAmount").isEqualTo(200)
        .jsonPath("advanceDate").isEqualTo(advance4Date.toString())
        .jsonPath("repaymentAmount").isEqualTo(100)
        .jsonPath("startDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("createdBy").isEqualTo("SA")
        .jsonPath("createDatetime").value<String> {
          assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
        }
        .jsonPath("informationNumber").isEqualTo("10002000-4")
        .jsonPath("advanceTransactionId").isEqualTo(24)
        .jsonPath("status").isEqualTo("WRITTEN_OFF")
    }

    @Test
    fun getAdvanceDoesNotExist() {
      webTestClient.get().uri("/finance/prisoners/advances/deduction-id/99999")
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
  @DisplayName("GET /finance/prisoners/advances/active-count")
  inner class GetPrisonerAdvancesCountTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/advances/active-count")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/advances/active-count")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/advances/active-count")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerAdvances() {
      webTestClient.get().uri("/finance/prisoners/advances/active-count")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("activeCount").isEqualTo(2)
    }
  }
}
