package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitorders

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.PVO_IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.time.LocalDate
import java.time.LocalDateTime

class VisitOrderResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderRepository: OffenderRepository

  @DisplayName("GET /prisoners/{offenderNo}/visit-orders/balance/to-migrate")
  @Nested
  inner class GetOffender {
    private lateinit var offender: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY", type = "GENERAL")
        }
        offender = offender(
          nomsId = "A1234BC",
          firstName = "JANE",
          lastName = "NARK",
          birthDate = LocalDate.parse("1999-12-22"),
          birthPlace = "LONDON",
          genderCode = "F",
          whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
        ) {
          booking {
            visitBalance { }
            visitBalanceAdjustment { }
            visitBalanceAdjustment(
              remainingVisitOrders = 5, 1, null, null,
              comment = "this is a comment",
              expiryBalance = 7,
              expiryDate = LocalDate.parse("2027-11-30"),
              endorsedStaffId = 234,
              authorisedStaffId = 123,
            )
            visitBalanceAdjustment(null, null, 3, 2, adjustmentReasonCode = PVO_IEP_ENTITLEMENT)
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      offenderRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance/to-migrate")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance/to-migrate")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance/to-migrate")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/prisoners/AB1234C/visit-orders/balance/to-migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_ORDERS")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return visit order balances`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance/to-migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_ORDERS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("remainingVisitOrders").isEqualTo(offender.latestBooking().visitBalance!!.remainingVisitOrders)
          .jsonPath("remainingPrivilegedVisitOrders").isEqualTo(offender.latestBooking().visitBalance!!.remainingPrivilegedVisitOrders)
      }

      @Test
      fun `will return visit order balance adjustments`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance/to-migrate")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_ORDERS")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("visitOrderBalanceAdjustments.length()").isEqualTo(offender.latestBooking().visitBalanceAdjustments.size)
      }

      @Test
      fun `is able to re-hydrate visit order balance`() {
        val visitOrderBalanceResponse =
          webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance/to-migrate")
            .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_ORDERS")))
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(PrisonerVisitOrderBalanceResponse::class.java).responseBody.blockFirst()!!

        assertThat(visitOrderBalanceResponse.remainingVisitOrders).isEqualTo(offender.latestBooking().visitBalance!!.remainingVisitOrders)
        assertThat(visitOrderBalanceResponse.remainingPrivilegedVisitOrders).isEqualTo(offender.latestBooking().visitBalance!!.remainingPrivilegedVisitOrders)

        assertThat(visitOrderBalanceResponse.visitOrderBalanceAdjustments.size).isEqualTo(3)
        with(visitOrderBalanceResponse.visitOrderBalanceAdjustments[0]) {
          assertThat(remainingVisitOrders).isEqualTo(4)
          assertThat(previousRemainingVisitOrders).isEqualTo(0)
          assertThat(remainingPrivilegedVisitOrders).isEqualTo(3)
          assertThat(previousRemainingPrivilegedVisitOrders).isEqualTo(0)
          assertThat(adjustmentReason!!.code).isEqualTo("IEP")
          assertThat(adjustmentReason!!.description).isEqualTo("IEP Entitlements")
          assertThat(adjustmentDate).isEqualTo(LocalDate.now())
          assertThat(comment).isNull()
          assertThat(expiryBalance).isNull()
          assertThat(expiryDate).isNull()
          assertThat(expiryStatus).isNull()
          assertThat(endorsedStaffId).isNull()
          assertThat(authorisedStaffId).isNull()
        }
        with(visitOrderBalanceResponse.visitOrderBalanceAdjustments[1]) {
          assertThat(remainingVisitOrders).isEqualTo(5)
          assertThat(previousRemainingVisitOrders).isEqualTo(1)
          assertThat(remainingPrivilegedVisitOrders).isNull()
          assertThat(previousRemainingPrivilegedVisitOrders).isNull()
          assertThat(adjustmentReason!!.code).isEqualTo("IEP")
          assertThat(adjustmentReason!!.description).isEqualTo("IEP Entitlements")
          assertThat(adjustmentDate).isEqualTo(LocalDate.now())
          assertThat(comment).isEqualTo("this is a comment")
          assertThat(expiryBalance).isEqualTo(7)
          assertThat(expiryDate).isEqualTo(LocalDate.parse("2027-11-30"))
          assertThat(expiryStatus).isNull()
          assertThat(endorsedStaffId).isEqualTo(234)
          assertThat(authorisedStaffId).isEqualTo(123)
        }
        with(visitOrderBalanceResponse.visitOrderBalanceAdjustments[2]) {
          assertThat(remainingVisitOrders).isNull()
          assertThat(previousRemainingVisitOrders).isNull()
          assertThat(remainingPrivilegedVisitOrders).isEqualTo(3)
          assertThat(previousRemainingPrivilegedVisitOrders).isEqualTo(2)
          assertThat(adjustmentReason!!.code).isEqualTo("PVO_IEP")
          assertThat(adjustmentReason!!.description).isEqualTo("PVO IEP Entitlements")
        }
      }
    }
  }
}
