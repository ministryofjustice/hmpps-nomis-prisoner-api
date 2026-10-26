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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.finance.scheduledpayments.PrisonerScheduledPaymentDto
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderScheduledPayment
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDate

@WithMockAuthUser
@TestInstance(PER_CLASS)
class PrisonerScheduledPaymentsResourceIntTest : IntegrationTestBase() {
  private lateinit var schedPay: OffenderScheduledPayment
  private lateinit var schedPay2: OffenderScheduledPayment
  private lateinit var schedPay3: OffenderScheduledPayment
  private var offender1Id: Long = 0
  private var offender2Id: Long = 0
  private var offender3Id: Long = 0

  @BeforeAll
  fun setUp() {
    nomisDataBuilder.build {
      offender1Id = offender {
        trustAccount()
        schedPay = scheduledPayment(reference = "REF1", comment = "COMMENT1", endDate = LocalDate.now().plusMonths(2))
      }.id
      offender2Id = offender(nomsId = "A1234BC").id
      offender3Id = offender(nomsId = "A6789CD") {
        trustAccount()
        schedPay2 = scheduledPayment()
        schedPay3 = scheduledPayment()
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
        .expectBodyList<PrisonerScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertThat(payments).hasSize(1)
      assertScheduledPayment(payments.single(), schedPay, "A5194DY")
    }

    @Test
    fun getPrisonerScheduledPaymentsEmptyList() {
      webTestClient.get().uri("/finance/prisoners/A1234BC/scheduled-payments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerScheduledPaymentDto>()
        .hasSize(0)
    }

    @Test
    fun getPrisonerScheduledPaymentsMultiple() {
      val payments = webTestClient.get().uri("/finance/prisoners/A6789CD/scheduled-payments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertThat(payments.map { it.id }).containsExactlyInAnyOrder(schedPay2.id, schedPay3.id)
      assertScheduledPayment(payments.single { it.id == schedPay2.id }, schedPay2, "A6789CD")
      assertScheduledPayment(payments.single { it.id == schedPay3.id }, schedPay3, "A6789CD")
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
        .expectBodyList<PrisonerScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertThat(payments).hasSize(1)
      assertScheduledPayment(payments.single(), schedPay, "A5194DY")
    }

    @Test
    fun getPrisonerScheduledPaymentsEmptyList() {
      webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender2Id/scheduled-payments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerScheduledPaymentDto>()
        .hasSize(0)
    }

    @Test
    fun getPrisonerScheduledPaymentsMultiple() {
      val payments = webTestClient.get().uri("/finance/prisoners/root-offender-id/$offender3Id/scheduled-payments")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBodyList<PrisonerScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertThat(payments.map { it.id }).containsExactlyInAnyOrder(schedPay2.id, schedPay3.id)
      assertScheduledPayment(payments.single { it.id == schedPay2.id }, schedPay2, "A6789CD")
      assertScheduledPayment(payments.single { it.id == schedPay3.id }, schedPay3, "A6789CD")
    }
  }

  @Nested
  @DisplayName("GET /finance/prisoners/scheduled-payments/{scheduledPaymentId}")
  inner class GetPrisonerScheduledPaymentTests {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/finance/prisoners/scheduled-payments/${schedPay.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/finance/prisoners/scheduled-payments/${schedPay.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/finance/prisoners/scheduled-payments/${schedPay.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Test
    fun getPrisonerScheduledPayments() {
      val payment = webTestClient.get().uri("/finance/prisoners/scheduled-payments/${schedPay.id}")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<PrisonerScheduledPaymentDto>()
        .returnResult()
        .responseBody!!

      assertScheduledPayment(payment, schedPay, "A5194DY")
    }

    @Test
    fun getPrisonerScheduledPaymentDoesNotExist() {
      webTestClient.get().uri("/finance/prisoners/scheduled-payments/99999")
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

  private fun assertScheduledPayment(
    payment: PrisonerScheduledPaymentDto,
    expected: OffenderScheduledPayment,
    prisonNumber: String,
  ) {
    assertThat(payment.id).isEqualTo(expected.id)
    assertThat(payment.prisonNumber).isEqualTo(prisonNumber)
    assertThat(payment.caseloadId).isEqualTo("MDI")
    assertThat(payment.transactionType).isEqualTo("TELE")
    assertThat(payment.amount).isEqualTo(245L)
    assertThat(payment.startDate).isEqualTo(expected.startDate)
    assertThat(payment.endDate).isEqualTo(expected.endDate)
    assertThat(payment.reference).isEqualTo(expected.referenceText)
    assertThat(payment.comment).isEqualTo(expected.commentText)
    assertThat(payment.createdBy).isEqualTo("SA")
    assertThat(payment.status).isEqualTo("TODO")
  }
}
