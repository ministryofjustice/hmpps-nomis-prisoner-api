package uk.gov.justice.digital.hmpps.nomisprisonerapi.finance

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.scheduledpayments.ScheduledPaymentDto
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledPayment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate

@WithMockAuthUser
@TestInstance(PER_CLASS)
class ScheduledPaymentsResourceIntTest : IntegrationTestBase() {
  private lateinit var schedPay1: OffenderScheduledPayment
  private lateinit var schedPay2: OffenderScheduledPayment
  private lateinit var schedPay3: OffenderScheduledPayment
  private lateinit var schedPay4: OffenderScheduledPayment
  private var offender1Id: Long = 0
  private var offender2Id: Long = 0
  private var offender3Id: Long = 0
  private val yesterday = LocalDate.now().minusDays(1)
  private val twoMonthsAgo = LocalDate.now().minusMonths(2)

  @BeforeAll
  fun setUp() {
    nomisDataBuilder.build {
      offender1Id = offender {
        trustAccount()
        schedPay1 = scheduledPayment(reference = "REF1", comment = "COMMENT1", endDate = LocalDate.now().plusMonths(2))
      }.id
      offender2Id = offender(nomsId = "A1234BC").id
      offender3Id = offender(nomsId = "A6789CD") {
        trustAccount()
        schedPay2 = scheduledPayment(transactionType = "NEWS")
        schedPay3 = scheduledPayment(caseloadId = "BXI", transactionType = "NEWS", startDate = LocalDate.now())
        // Inactive
        schedPay4 = scheduledPayment(caseloadId = "BXI", transactionType = "NEWS", startDate = twoMonthsAgo, endDate = yesterday)
        advance(informationNumber = "12341234-1", deductionPriority = 1)
      }.id
    }
  }

  @AfterAll
  fun tearDown() {
    deleteOffenders()
  }

  @Nested
  @DisplayName("GET /finance/prisoners/{prisonNumber}/scheduled-payments")
  inner class PrisonerScheduledPaymentsByPrisonNumberTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/A5194DY/scheduled-payments")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/A5194DY/scheduled-payments")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/A5194DY/scheduled-payments")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerScheduledPayments() {
      val payments = webTestClient.get().uri("/finance/prisoners/A5194DY/scheduled-payments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertThat(payments).hasSize(1)
      assertScheduledPayment1(payments)
    }

    @Test
    fun getPrisonerScheduledPaymentsEmptyList() {
      webTestClient.get().uri("/finance/prisoners/A1234BC/scheduled-payments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ScheduledPaymentDto>()
        .hasSize(0)
    }

    @Test
    fun getPrisonerScheduledPaymentsMultiple() {
      val payments = webTestClient.get().uri("/finance/prisoners/A6789CD/scheduled-payments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertThat(payments.size).isEqualTo(3)
      assertThat(payments.map { it.id }).containsExactlyInAnyOrder(schedPay2.id, schedPay3.id, schedPay4.id)
      assertScheduledPayment2(payments)
      assertScheduledPayment3(payments)
      assertScheduledPayment4(payments)
    }

    @Test
    fun getActivePrisonerScheduledPayments() {
      val payments = webTestClient.get().uri("/finance/prisoners/A6789CD/scheduled-payments?activeOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertThat(payments).hasSize(2)
      assertScheduledPayment2(payments)
      assertScheduledPayment3(payments)
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/root-offender-id/{rootOffenderId}/scheduled-payments")
  inner class PrisonerScheduledPaymentsByRootOffenderIdTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender1Id/scheduled-payments")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender1Id/scheduled-payments")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender1Id/scheduled-payments")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerScheduledPayments() {
      val payments = webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender1Id/scheduled-payments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertThat(payments).hasSize(1)
      assertScheduledPayment1(payments)
    }

    @Test
    fun getPrisonerScheduledPaymentsEmptyList() {
      webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender2Id/scheduled-payments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ScheduledPaymentDto>()
        .hasSize(0)
    }

    @Test
    fun getPrisonerScheduledPaymentsMultiple() {
      val payments = webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender3Id/scheduled-payments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertThat(payments).hasSize(3)
      assertThat(payments.map { it.id }).containsExactlyInAnyOrder(schedPay2.id, schedPay3.id, schedPay4.id)
      assertScheduledPayment2(payments)
      assertScheduledPayment3(payments)
      assertScheduledPayment4(payments)
    }

    @Test
    fun getActivePrisonerScheduledPayments() {
      val payments = webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender3Id/scheduled-payments?activeOnly=true")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<ScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertThat(payments).hasSize(2)
      assertScheduledPayment2(payments)
      assertScheduledPayment3(payments)
    }
  }

  @Nested
  @DisplayName("GET /finance/scheduled-payments/{scheduledPaymentId}")
  inner class GetScheduledPaymentTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/scheduled-payments/${schedPay1.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/scheduled-payments/${schedPay1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/scheduled-payments/${schedPay1.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getScheduledPayment() {
      val payment = webTestClient.get().uri("/finance/scheduled-payments/${schedPay1.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<ScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertScheduledPayment1(listOf(payment))
    }

    @Test
    fun getScheduledPaymentDoesNotExist() {
      webTestClient.get().uri("/finance/scheduled-payments/99999")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isNotFound
        .expectBody()
        .jsonPath("userMessage").value<String> {
          assertThat(it).contains("Not Found: Offender scheduled payment with id 99999 not found")
        }
    }
  }

  @Nested
  @DisplayName("GET /finance/scheduled-payments/active-count")
  inner class GetActiveScheduledPaymentsCountTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/scheduled-payments/active-count")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/scheduled-payments/active-count")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/scheduled-payments/active-count")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getActiveScheduledPaymentsCount() {
      webTestClient.get().uri("/finance/scheduled-payments/active-count")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody()
        .jsonPath("activeCount").isEqualTo(3)
    }
  }

  private fun assertScheduledPayment1(payments: List<ScheduledPaymentDto>) = assertScheduledPayment(
    payment = payments.single { it.id == schedPay1.id },
    expected = schedPay1,
    accountCode = 2101,
    dayOfWeek = WeekDay.TUE,
  )

  private fun assertScheduledPayment2(payments: List<ScheduledPaymentDto>) = assertScheduledPayment(
    payment = payments.single { it.id == schedPay2.id },
    expected = schedPay2,
    accountCode = 2103,
    dayOfWeek = WeekDay.MON,
  )

  private fun assertScheduledPayment3(payments: List<ScheduledPaymentDto>) = assertScheduledPayment(
    payment = payments.single { it.id == schedPay3.id },
    expected = schedPay3,
    accountCode = 2102,
    dayOfWeek = WeekDay.FRI,
  )

  private fun assertScheduledPayment4(payments: List<ScheduledPaymentDto>) = assertScheduledPayment(
    payment = payments.single { it.id == schedPay4.id },
    expected = schedPay4,
    accountCode = 2102,
    dayOfWeek = WeekDay.FRI,
  )

  private fun assertScheduledPayment(
    payment: ScheduledPaymentDto,
    expected: OffenderScheduledPayment,
    accountCode: Int,
    dayOfWeek: WeekDay,
  ) {
    assertThat(payment.id).isEqualTo(expected.id)
    assertThat(payment.prisonNumber).isEqualTo(expected.offender.nomsId)
    assertThat(payment.caseloadId).isEqualTo(expected.caseloadId)
    assertThat(payment.transactionType).isEqualTo(expected.transactionType)
    assertThat(payment.amount).isEqualTo(MoneySupport.poundsToPence(expected.paymentAmount))
    assertThat(payment.startDate).isEqualTo(expected.startDate)
    assertThat(payment.endDate).isEqualTo(expected.endDate)
    assertThat(payment.reference).isEqualTo(expected.referenceText)
    assertThat(payment.comment).isEqualTo(expected.commentText)
    assertThat(payment.createdBy).isEqualTo("SA")
    assertThat(payment.dayOfWeek).isEqualTo(dayOfWeek)
    assertThat(payment.subAccountCode).isEqualTo(accountCode)
  }
}
