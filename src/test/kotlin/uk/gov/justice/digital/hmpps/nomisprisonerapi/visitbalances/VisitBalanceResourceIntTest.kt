@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances

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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.PVO_IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.time.LocalDate
import java.time.LocalDateTime

class VisitBalanceResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderRepository: OffenderRepository

  @DisplayName("GET /prisoners/{offenderNo}/visit-orders/balance")
  @Nested
  inner class GetOffenderVisitBalance {
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
              remainingVisitOrders = 5,
              previousRemainingVisitOrders = 1,
              remainingPrivilegedVisitOrders = null,
              previousRemainingPrivilegedVisitOrders = null,
              comment = "this is a comment",
              expiryBalance = 7,
              expiryDate = LocalDate.parse("2027-11-30"),
              endorsedStaffId = 234,
              authorisedStaffId = 123,
            )
            visitBalanceAdjustment(
              remainingVisitOrders = null,
              previousRemainingVisitOrders = null,
              remainingPrivilegedVisitOrders = 3,
              previousRemainingPrivilegedVisitOrders = 2,
              adjustmentReasonCode = PVO_IEP_ENTITLEMENT,
            )
            visitBalanceAdjustment(
              remainingVisitOrders = null,
              previousRemainingVisitOrders = null,
              remainingPrivilegedVisitOrders = 4,
              previousRemainingPrivilegedVisitOrders = 1,
              adjustmentReasonCode = PVO_IEP_ENTITLEMENT,
              adjustmentDate = LocalDate.now().minusMonths(1).minusDays(1),
            )
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
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/prisoners/AB1234C/visit-orders/balance")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return visit order balances`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("remainingVisitOrders").isEqualTo(offender.latestBooking().visitBalance!!.remainingVisitOrders!!)
          .jsonPath("remainingPrivilegedVisitOrders").isEqualTo(offender.latestBooking().visitBalance!!.remainingPrivilegedVisitOrders!!)
      }

      @Test
      fun `will return visit order balance adjustments`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("visitOrderBalanceAdjustments.length()").isEqualTo(3)
      }

      @Test
      fun `is able to re-hydrate visit order balance`() {
        val visitOrderBalanceResponse =
          webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance")
            .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
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
          assertThat(endorsedStaffId).isNull()
          assertThat(authorisedStaffId).isNull()
          assertThat(createUsername).isEqualTo("SA")
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
          assertThat(endorsedStaffId).isEqualTo(234)
          assertThat(authorisedStaffId).isEqualTo(123)
          assertThat(createUsername).isEqualTo("SA")
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

  @DisplayName("GET /visit-balances/ids")
  @Nested
  inner class getVisitBalanceIds {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff(firstName = "KOFE", lastName = "ADDY") {
          account(username = "KOFEADDY", type = "GENERAL")
        }
        offender(nomsId = "A1234BC") {
          booking {
            visitBalance { }
          }
          booking {
            visitBalance { }
          }
        }
        offender(nomsId = "A1234CD") {
          booking {
            visitBalance { }
          }
        }
        offender(nomsId = "A1234EF") {
          booking(agencyLocationId = "MDI") {
            visitBalance { }
          }
          booking {
            visitBalance { }
          }
        }

        offender(nomsId = "A1234GH") { booking() }
        offender(nomsId = "A1234IJ")
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
        webTestClient.get().uri("/visit-balances/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/visit-balances/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/visit-balances/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return count of all visit balances by default`() {
        webTestClient.get().uri("/visit-balances/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(3)
          .jsonPath("numberOfElements").isEqualTo(3)
      }

      @Test
      fun `will return a page of  visit balances`() {
        webTestClient.get().uri("/visit-balances/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalElements").isEqualTo(3)
          .jsonPath("numberOfElements").isEqualTo(3)
          .jsonPath("content[0].visitBalanceId").isNumber
          .jsonPath("content[1].visitBalanceId").isNumber
          .jsonPath("content[2].visitBalanceId").isNumber
      }

      @Test
      fun `will page the data`() {
        webTestClient.get().uri("/visit-balances/ids?size=1&page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalPages").isEqualTo(3)
          .jsonPath("totalElements").isEqualTo(3)
          .jsonPath("pageable.pageSize").isEqualTo(1)
          .jsonPath("numberOfElements").isEqualTo(1)
      }

      @Test
      fun `can filter by prison Id`() {
        webTestClient.get().uri("/visit-balances/ids?prisonId=MDI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("totalPages").isEqualTo(1)
          .jsonPath("numberOfElements").isEqualTo(1)
      }
    }
  }

  @DisplayName("GET /visit-orders/visit-balance-adjustment/{visitBalanceAdjustmentId}")
  @Nested
  inner class getVisitBalanceAdjustment {
    private lateinit var adjustmentMin: OffenderVisitBalanceAdjustment
    private lateinit var adjustment: OffenderVisitBalanceAdjustment

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender {
          booking {
            adjustmentMin = visitBalanceAdjustment(
              remainingVisitOrders = null,
              previousRemainingVisitOrders = null,
              remainingPrivilegedVisitOrders = null,
              previousRemainingPrivilegedVisitOrders = null,
              adjustmentDate = LocalDate.parse("2021-11-30"),
            )
            adjustment = visitBalanceAdjustment(
              remainingVisitOrders = 1,
              previousRemainingVisitOrders = 2,
              remainingPrivilegedVisitOrders = 3,
              previousRemainingPrivilegedVisitOrders = 4,
              adjustmentDate = LocalDate.parse("2021-11-30"),
              adjustmentReasonCode = PVO_IEP_ENTITLEMENT,
              comment = "this is a comment",
              expiryBalance = 7,
              expiryDate = LocalDate.parse("2027-11-30"),
              endorsedStaffId = 234,
              authorisedStaffId = 123,
            )
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
        webTestClient.get().uri("/visit-orders/visit-balance-adjustment/${adjustment.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/visit-orders/visit-balance-adjustment/${adjustment.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/visit-orders/visit-balance-adjustment/${adjustment.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when adjustment not found`() {
        webTestClient.get().uri("/visit-orders/visit-balance-adjustment/12345")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return minimal visit balance adjustment`() {
        webTestClient.get().uri("/visit-orders/visit-balance-adjustment/${adjustmentMin.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("remainingVisitOrders").doesNotExist()
          .jsonPath("previousRemainingVisitOrders").doesNotExist()
          .jsonPath("remainingPrivilegedVisitOrders").doesNotExist()
          .jsonPath("previousRemainingPrivilegedVisitOrders").doesNotExist()
          .jsonPath("adjustmentReason.code").isEqualTo("IEP")
          .jsonPath("adjustmentReason.description").isEqualTo("IEP Entitlements")
          .jsonPath("adjustmentDate").isEqualTo("2021-11-30")
          .jsonPath("comment").doesNotExist()
          .jsonPath("expiryBalance").doesNotExist()
          .jsonPath("expiryDate").doesNotExist()
      }

      @Test
      fun `will return visit balance adjustment fully populated`() {
        webTestClient.get().uri("/visit-orders/visit-balance-adjustment/${adjustment.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("remainingVisitOrders").isEqualTo(1)
          .jsonPath("previousRemainingVisitOrders").isEqualTo(2)
          .jsonPath("remainingPrivilegedVisitOrders").isEqualTo(3)
          .jsonPath("previousRemainingPrivilegedVisitOrders").isEqualTo(4)
          .jsonPath("adjustmentReason.code").isEqualTo(PVO_IEP_ENTITLEMENT)
          .jsonPath("adjustmentReason.description").isEqualTo("PVO IEP Entitlements")
          .jsonPath("adjustmentDate").isEqualTo("2021-11-30")
          .jsonPath("comment").isEqualTo("this is a comment")
          .jsonPath("expiryBalance").isEqualTo(7)
          .jsonPath("expiryDate").isEqualTo("2027-11-30")
      }
    }
  }
}
