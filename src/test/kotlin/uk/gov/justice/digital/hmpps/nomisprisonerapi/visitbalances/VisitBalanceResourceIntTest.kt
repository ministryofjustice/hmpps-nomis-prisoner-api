@file:Suppress("ClassName")

package uk.gov.justice.digital.hmpps.nomisprisonerapi.visitbalances

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderVisitBalanceAdjustment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.VisitOrderAdjustmentReason.Companion.PVO_IEP_ENTITLEMENT
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import java.time.LocalDate
import java.time.LocalDateTime

class VisitBalanceResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderBookingRepository: OffenderBookingRepository

  @Autowired
  private lateinit var repository: Repository

  @DisplayName("POST /prisoners/{prisonNumber}/visit-balance-adjustments")
  @Nested
  inner class CreateVisitBalanceAdjustment {
    private var activeBookingId = 0L
    private var bookingWithNoVisitBalanceId = 0L
    private lateinit var omsOwner: Staff
    private lateinit var staffUser: Staff
    private lateinit var prisoner: Offender
    private val validAdjustment = CreateVisitBalanceAdjustmentRequest(
      visitOrderChange = 3,
      adjustmentDate = LocalDate.parse("2025-03-03"),
    )

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        omsOwner = staff(firstName = "OMS", lastName = "OWNER") {
          account(username = "OMS_OWNER")
        }
        staffUser = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }

        prisoner = offender(nomsId = "A1234AB") {
          activeBookingId = booking {
            visitBalanceAdjustment(
              privilegedVisitOrderChange = 5,
              previousPrivilegedVisitOrderCount = 6,
            )
            visitBalanceAdjustment(
              visitOrderChange = 11,
            )
          }.bookingId
          booking(bookingBeginDate = LocalDateTime.parse("2021-07-18T10:00:00")) {
            visitBalanceAdjustment(
              visitOrderChange = 3,
              previousVisitOrderCount = 7,
              privilegedVisitOrderChange = 2,
              previousPrivilegedVisitOrderCount = 1,
              adjustmentDate = LocalDate.parse("2025-03-03"),
              adjustmentReasonCode = "GOV",
              comment = "Good behaviour",
              expiryBalance = 7,
              expiryDate = LocalDate.parse("2025-03-23"),
              endorsedStaffId = 123,
              authorisedStaffId = 456,
            )
          }
        }
        offender(nomsId = "A5678CD") {
          bookingWithNoVisitBalanceId = booking().bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
      repository.deleteStaff()
    }

    @Nested
    inner class Security {

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoners/A1234AB/visit-balance-adjustments")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validAdjustment)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/A1234AB/visit-balance-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validAdjustment)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/A1234AB/visit-balance-adjustments")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validAdjustment)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private val validVisitBalanceAdjustment = CreateVisitBalanceAdjustmentRequest(
        visitOrderChange = 3,
        adjustmentDate = LocalDate.parse("2025-03-03"),
      )

      @Test
      fun `validation fails when prisoner does not exist`() {
        webTestClient.post().uri("/prisoners/A9999ZZ/visit-balance-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validVisitBalanceAdjustment)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `validation fails when visit-balance-adjustment username is not valid`() {
        webTestClient.post().uri("/prisoners/A1234AB/visit-balance-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "visitOrderChange" : 3,
                "authorisedUsername": "INVALID",
                "adjustmentDate": "2025-03-03"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      private val validFullAdjustment = CreateVisitBalanceAdjustmentRequest(
        visitOrderChange = 3,
        previousVisitOrderCount = 7,
        privilegedVisitOrderChange = 2,
        previousPrivilegedVisitOrderCount = 1,
        adjustmentDate = LocalDate.parse("2025-03-03"),
        comment = "Good behaviour",
      )

      @Test
      fun `creating a visit balance adjustment with minimal data will be successful`() {
        webTestClient.post().uri("/prisoners/A1234AB/visit-balance-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "adjustmentReasonCode": "DISC",
                "adjustmentDate": "2025-03-03"
              }
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isEqualTo(201)
          .expectBody()
          .jsonPath("visitBalanceAdjustmentId").isNotEmpty
      }

      @Test
      fun `creating a visit balance adjustment will allow the data to be retrieved`() {
        webTestClient.post().uri("/prisoners/A1234AB/visit-balance-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validFullAdjustment)
          .exchange()
          .expectStatus().isEqualTo(201)

        repository.runInTransaction {
          val booking = offenderBookingRepository.findByIdOrNull(activeBookingId)
          assertThat(booking?.visitBalanceAdjustments).hasSize(3)
          val newAdjustment = booking?.visitBalanceAdjustments?.last()!!
          assertThat(newAdjustment.id).isNotNull()
          assertThat(newAdjustment.remainingVisitOrders).isEqualTo(3)
          assertThat(newAdjustment.previousRemainingVisitOrders).isEqualTo(7)
          assertThat(newAdjustment.remainingPrivilegedVisitOrders).isEqualTo(2)
          assertThat(newAdjustment.previousRemainingPrivilegedVisitOrders).isEqualTo(1)
          assertThat(newAdjustment.adjustReasonCode.code).isEqualTo("VO_ISSUE")
          assertThat(newAdjustment.authorisedStaffId).isEqualTo(omsOwner.id)
          assertThat(newAdjustment.endorsedStaffId).isEqualTo(omsOwner.id)
          assertThat(newAdjustment.adjustDate).isEqualTo(LocalDate.parse("2025-03-03"))
          assertThat(newAdjustment.commentText).isEqualTo("Good behaviour")
          assertThat(newAdjustment.expiryBalance).isNull()
          assertThat(newAdjustment.expiryDate).isNull()
        }
      }

      @Test
      fun `creating a visit balance adjustment with minimal data will allow the data to be retrieved`() {
        webTestClient.post().uri("/prisoners/A1234AB/visit-balance-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            CreateVisitBalanceAdjustmentRequest(
              adjustmentDate = LocalDate.parse("2025-03-03"),
              authorisedUsername = "JANESTAFF",
            ),
          )
          .exchange()
          .expectStatus().isEqualTo(201)

        repository.runInTransaction {
          val booking = offenderBookingRepository.findByIdOrNull(activeBookingId)
          assertThat(booking?.visitBalanceAdjustments).hasSize(3)
          val newAdjustment = booking?.visitBalanceAdjustments?.last()!!
          assertThat(newAdjustment.id).isNotNull()
          assertThat(newAdjustment.remainingVisitOrders).isNull()
          assertThat(newAdjustment.previousRemainingVisitOrders).isNull()
          assertThat(newAdjustment.remainingPrivilegedVisitOrders).isNull()
          assertThat(newAdjustment.previousRemainingPrivilegedVisitOrders).isNull()
          assertThat(newAdjustment.adjustReasonCode.code).isEqualTo("PVO_ISSUE")
          assertThat(newAdjustment.authorisedStaffId).isEqualTo(staffUser.id)
          assertThat(newAdjustment.endorsedStaffId).isEqualTo(staffUser.id)
          assertThat(newAdjustment.adjustDate).isEqualTo(LocalDate.parse("2025-03-03"))
          assertThat(newAdjustment.commentText).isNull()
          assertThat(newAdjustment.expiryBalance).isNull()
          assertThat(newAdjustment.expiryDate).isNull()
        }
      }

      @Test
      fun `creating a visit balance adjustment when no visit balance exists successfully create a new balance`() {
        val adjustment = CreateVisitBalanceAdjustmentRequest(
          visitOrderChange = 3,
          privilegedVisitOrderChange = 2,
          adjustmentDate = LocalDate.parse("2025-03-03"),
          comment = "Good behaviour",
        )

        webTestClient.post().uri("/prisoners/A5678CD/visit-balance-adjustments")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(adjustment)
          .exchange()
          .expectStatus().isEqualTo(201)
          .expectBody()
          .jsonPath("visitBalanceAdjustmentId").isNotEmpty

        repository.runInTransaction {
          val booking = offenderBookingRepository.findByIdOrNull(bookingWithNoVisitBalanceId)!!
          assertThat(booking.visitBalanceAdjustments).hasSize(1)
          val newAdjustment = booking.visitBalanceAdjustments.first()
          assertThat(newAdjustment.id).isNotNull()

          assertThat(newAdjustment.remainingVisitOrders).isEqualTo(3)
          assertThat(newAdjustment.remainingPrivilegedVisitOrders).isEqualTo(2)

          // Check that visit balance exists
          assertThat(booking.visitBalance!!.remainingVisitOrders).isNotNull
          assertThat(booking.visitBalance!!.remainingPrivilegedVisitOrders).isNotNull
        }
      }
    }
  }

  @DisplayName("PUT /prisoners/{prisonNumber}/visit-balance")
  @Nested
  inner class UpdateVisitBalance {
    private var activeBookingId = 0L
    private var bookingWithNoVisitBalanceId = 0L
    private lateinit var prisoner: Offender
    private lateinit var prisoner2: Offender
    private val validAdjustment = UpdateVisitBalanceRequest(
      remainingVisitOrders = 3,
      remainingPrivilegedVisitOrders = 7,
    )

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        prisoner = offender(nomsId = "A1234AB") {
          activeBookingId = booking {
            visitBalance(
              remainingVisitOrders = 5,
              remainingPrivilegedVisitOrders = 6,
            )
            visitBalance(
              remainingVisitOrders = 11,
            )
          }.bookingId
          booking(bookingBeginDate = LocalDateTime.parse("2021-07-18T10:00:00")) {
            visitBalance(
              remainingVisitOrders = 3,
              remainingPrivilegedVisitOrders = 4,
            )
          }
        }
        prisoner2 = offender(nomsId = "A4321AB") {
          bookingWithNoVisitBalanceId = booking().bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class Security {

      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/prisoners/A1234AB/visit-balance")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validAdjustment)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/prisoners/A1234AB/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validAdjustment)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/prisoners/A1234AB/visit-balance")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validAdjustment)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private val validVisitBalance = UpdateVisitBalanceRequest(
        remainingVisitOrders = 13,
        remainingPrivilegedVisitOrders = 14,
      )

      @Test
      fun `validation fails when prisoner does not exist`() {
        webTestClient.put().uri("/prisoners/A9999ZZ/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validVisitBalance)
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      private val validFullBalance = UpdateVisitBalanceRequest(
        remainingVisitOrders = 13,
        remainingPrivilegedVisitOrders = 14,
      )

      @Test
      fun `updating a visit balance will allow the data to be retrieved`() {
        webTestClient.put().uri("/prisoners/A1234AB/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validFullBalance)
          .exchange()
          .expectStatus().isEqualTo(204)

        repository.runInTransaction {
          val booking = offenderBookingRepository.findByIdOrNull(activeBookingId)
          val newBalance = booking?.visitBalance!!
          assertThat(newBalance.remainingVisitOrders).isEqualTo(13)
          assertThat(newBalance.remainingPrivilegedVisitOrders).isEqualTo(14)
        }
      }

      @Test
      fun `creating a visit balance will allow the data to be retrieved`() {
        webTestClient.put().uri("/prisoners/A4321AB/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validFullBalance)
          .exchange()
          .expectStatus().isEqualTo(204)

        repository.runInTransaction {
          val booking = offenderBookingRepository.findByIdOrNull(bookingWithNoVisitBalanceId)
          val newBalance = booking?.visitBalance!!
          assertThat(newBalance.remainingVisitOrders).isEqualTo(13)
          assertThat(newBalance.remainingPrivilegedVisitOrders).isEqualTo(14)
        }
      }

      @Test
      fun `passing in no visit balance when no existing balance will cause no action`() {
        webTestClient.put().uri("/prisoners/${prisoner2.nomsId}/visit-balance")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_VISIT_BALANCE")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(UpdateVisitBalanceRequest(null, null))
          .exchange()
          .expectStatus().isEqualTo(204)

        repository.runInTransaction {
          val booking = offenderBookingRepository.findByIdOrNull(bookingWithNoVisitBalanceId)
          assertThat(booking?.visitBalance).isNull()
        }
      }
    }
  }

  @DisplayName("GET /visit-balances/{visitBalanceId}")
  @Nested
  inner class getVisitBalanceByIdToMigrate {
    private lateinit var offender: Offender
    private lateinit var booking: OffenderBooking
    private var bookingIdWithNullVisitOrders: Long = 0

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
        offender(nomsId = "A1234DE") {
          bookingIdWithNullVisitOrders = booking {
            visitBalance(
              remainingVisitOrders = null,
              remainingPrivilegedVisitOrders = null,
            )
          }.bookingId
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
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
        webTestClient.get().uri("/visit-balances/9999")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return zero balances when null entries for vos or pvos`() {
        webTestClient.get().uri("/visit-balances/$bookingIdWithNullVisitOrders")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("remainingVisitOrders").isEqualTo(0)
          .jsonPath("remainingPrivilegedVisitOrders").isEqualTo(0)
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
            .returnResult(VisitBalanceDetailResponse::class.java).responseBody.blockFirst()!!

        assertThat(visitOrderBalanceResponse.remainingVisitOrders).isEqualTo(offender.latestBooking().visitBalance!!.remainingVisitOrders)
        assertThat(visitOrderBalanceResponse.remainingPrivilegedVisitOrders).isEqualTo(offender.latestBooking().visitBalance!!.remainingPrivilegedVisitOrders)
        assertThat(visitOrderBalanceResponse.lastIEPAllocationDate).isEqualTo("2025-03-12")
      }
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/visit-balance")
  @Nested
  inner class getVisitBalance {
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
          }
        }
        offender(
          nomsId = "A5432BC",
          firstName = "JIM",
          lastName = "PARK",
          birthDate = LocalDate.parse("1999-12-22"),
          birthPlace = "LONDON",
          genderCode = "F",
          whenCreated = LocalDateTime.parse("2020-01-01T10:00"),
        ) {
          booking() // no visit balance
        }
        offender(nomsId = "A1234DE") {
          booking {
            visitBalance(
              remainingVisitOrders = null,
              remainingPrivilegedVisitOrders = null,
            )
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteOffenders()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-balance")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-balance")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-balance")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when offender not found`() {
        webTestClient.get().uri("/prisoners/AB1234C/visit-balance")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return null when no visit balance found`() {
        webTestClient.get().uri("/prisoners/A5432BC/visit-balance")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus().isOk
          .expectBody().isEmpty
      }

      @Test
      fun `return null when null entries for vos or pvos`() {
        webTestClient.get().uri("/prisoners/A1234DE/visit-balance")
          .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .isEmpty
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return visit order balances`() {
        webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-balance")
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
          webTestClient.get().uri("/prisoners/${offender.nomsId}/visit-balance")
            .headers(setAuthorisation(roles = listOf("NOMIS_VISIT_BALANCE")))
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(VisitBalanceResponse::class.java).responseBody.blockFirst()!!

        assertThat(visitOrderBalanceResponse.remainingVisitOrders).isEqualTo(offender.latestBooking().visitBalance!!.remainingVisitOrders)
        assertThat(visitOrderBalanceResponse.remainingPrivilegedVisitOrders).isEqualTo(offender.latestBooking().visitBalance!!.remainingPrivilegedVisitOrders)
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
      repository.deleteOffenders()
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
      repository.deleteOffenders()
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
