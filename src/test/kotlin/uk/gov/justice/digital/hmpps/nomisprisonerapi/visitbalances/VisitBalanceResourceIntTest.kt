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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.PVO_IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import java.time.LocalDate
import java.time.LocalDateTime

class VisitBalanceResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderRepository: OffenderRepository

  @DisplayName("GET /visit-balances/{visitBalanceId}")
  @Nested
  inner class getVisitBalanceByIdToMigrate {
    private lateinit var offender: Offender
    private lateinit var booking: OffenderBooking

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender = offender(
          nomsId = "A1234BC",
          firstName = "JANE",
          lastName = "NARK",
          birthDate = LocalDate.parse("1999-12-22"),
          birthPlace = "LONDON",
          genderCode = "F",
          whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
        ) {
          booking = booking {
            visitBalance { }
            visitBalanceAdjustment { }
            visitBalanceAdjustment(
              visitOrderChange = 5,
              previousVisitOrderCount = 1,
              privilegedVisitOrderChange = null,
              previousPrivilegedVisitOrderCount = null,
              adjustmentReasonCode = IEP_ENTITLEMENT,
              adjustmentDate = LocalDate.parse("2025-03-12"),
              comment = "this is a comment for the most recent batch iep adjustment",
              expiryBalance = 7,
              expiryDate = LocalDate.parse("2027-11-30"),
              endorsedStaffId = 234,
              authorisedStaffId = 123,
            )
            visitBalanceAdjustment(
              visitOrderChange = null,
              previousVisitOrderCount = null,
              privilegedVisitOrderChange = 3,
              previousPrivilegedVisitOrderCount = 2,
              adjustmentReasonCode = PVO_IEP_ENTITLEMENT,
              adjustmentDate = LocalDate.parse("2025-01-11"),
            )
            visitBalanceAdjustment(
              visitOrderChange = null,
              previousVisitOrderCount = null,
              privilegedVisitOrderChange = 4,
              previousPrivilegedVisitOrderCount = 1,
              adjustmentReasonCode = PVO_IEP_ENTITLEMENT,
              adjustmentDate = LocalDate.parse("2025-02-10"),
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
        webTestClient.get().uri("/visit-balances/${booking.bookingId}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/visit-balances/${booking.bookingId}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/visit-balances/${booking.bookingId}")
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
        webTestClient.get().uri("/visit-balances/${booking.bookingId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("remainingVisitOrders").isEqualTo(offender.latestBooking().visitBalance!!.remainingVisitOrders!!)
          .jsonPath("remainingPrivilegedVisitOrders").isEqualTo(offender.latestBooking().visitBalance!!.remainingPrivilegedVisitOrders!!)
      }

      @Test
      fun `is able to re-hydrate visit order balance`() {
        val visitOrderBalanceResponse =
          webTestClient.get().uri("/visit-balances/${booking.bookingId}")
            .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(PrisonerVisitBalanceResponse::class.java).responseBody.blockFirst()!!

        assertThat(visitOrderBalanceResponse.remainingVisitOrders).isEqualTo(offender.latestBooking().visitBalance!!.remainingVisitOrders)
        assertThat(visitOrderBalanceResponse.remainingPrivilegedVisitOrders).isEqualTo(offender.latestBooking().visitBalance!!.remainingPrivilegedVisitOrders)
        assertThat(visitOrderBalanceResponse.lastIEPAllocationDate).isEqualTo(LocalDate.parse("2025-03-12"))
      }
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/visit-orders/balance")
  @Nested
  inner class getVisitBalanceToMigrate {
    private lateinit var offender: Offender

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
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
              visitOrderChange = 5,
              previousVisitOrderCount = 1,
              privilegedVisitOrderChange = null,
              previousPrivilegedVisitOrderCount = null,
              adjustmentReasonCode = IEP_ENTITLEMENT,
              adjustmentDate = LocalDate.now().minusDays(1),
              comment = "this is a comment for the most recent batch iep adjustment",
              expiryBalance = 7,
              expiryDate = LocalDate.parse("2027-11-30"),
              endorsedStaffId = 234,
              authorisedStaffId = 123,
            )
            visitBalanceAdjustment(
              visitOrderChange = null,
              previousVisitOrderCount = null,
              privilegedVisitOrderChange = 3,
              previousPrivilegedVisitOrderCount = 2,
              adjustmentReasonCode = PVO_IEP_ENTITLEMENT,
              adjustmentDate = LocalDate.now().minusMonths(5),
            )
            visitBalanceAdjustment(
              visitOrderChange = null,
              previousVisitOrderCount = null,
              privilegedVisitOrderChange = 4,
              previousPrivilegedVisitOrderCount = 1,
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
      fun `is able to re-hydrate visit order balance`() {
        val visitOrderBalanceResponse =
          webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-orders/balance")
            .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(PrisonerVisitBalanceResponse::class.java).responseBody.blockFirst()!!

        assertThat(visitOrderBalanceResponse.remainingVisitOrders).isEqualTo(offender.latestBooking().visitBalance!!.remainingVisitOrders)
        assertThat(visitOrderBalanceResponse.remainingPrivilegedVisitOrders).isEqualTo(offender.latestBooking().visitBalance!!.remainingPrivilegedVisitOrders)
        assertThat(visitOrderBalanceResponse.lastIEPAllocationDate).isEqualTo(LocalDate.now().minusDays(1))
      }
    }
  }

  @DisplayName("GET /visit-balances/ids")
  @Nested
  inner class getVisitBalanceIds {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
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

  @DisplayName("GET /visit-balances/visit-balance-adjustment/{visitBalanceAdjustmentId}")
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
              visitOrderChange = null,
              previousVisitOrderCount = null,
              privilegedVisitOrderChange = null,
              previousPrivilegedVisitOrderCount = null,
              adjustmentDate = LocalDate.parse("2021-11-30"),
            )
            adjustment = visitBalanceAdjustment(
              visitOrderChange = 1,
              previousVisitOrderCount = 22,
              privilegedVisitOrderChange = 3,
              previousPrivilegedVisitOrderCount = 24,
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
        webTestClient.get().uri("/visit-balances/visit-balance-adjustment/${adjustment.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/visit-balances/visit-balance-adjustment/${adjustment.id}")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/visit-balances/visit-balance-adjustment/${adjustment.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when adjustment not found`() {
        webTestClient.get().uri("/visit-balances/visit-balance-adjustment/12345")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return minimal visit balance adjustment`() {
        webTestClient.get().uri("/visit-balances/visit-balance-adjustment/${adjustmentMin.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("visitOrderChange").doesNotExist()
          .jsonPath("previousVisitOrderCount").doesNotExist()
          .jsonPath("privilegedVisitOrderChange").doesNotExist()
          .jsonPath("previousPrivilegedVisitOrderCount").doesNotExist()
          .jsonPath("adjustmentReason.code").isEqualTo("IEP")
          .jsonPath("adjustmentReason.description").isEqualTo("IEP Entitlements")
          .jsonPath("adjustmentDate").isEqualTo("2021-11-30")
          .jsonPath("comment").doesNotExist()
          .jsonPath("expiryBalance").doesNotExist()
          .jsonPath("expiryDate").doesNotExist()
      }

      @Test
      fun `will return visit balance adjustment fully populated`() {
        webTestClient.get().uri("/visit-balances/visit-balance-adjustment/${adjustment.id}")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("visitOrderChange").isEqualTo(1)
          .jsonPath("previousVisitOrderCount").isEqualTo(22)
          .jsonPath("privilegedVisitOrderChange").isEqualTo(3)
          .jsonPath("previousPrivilegedVisitOrderCount").isEqualTo(24)
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
