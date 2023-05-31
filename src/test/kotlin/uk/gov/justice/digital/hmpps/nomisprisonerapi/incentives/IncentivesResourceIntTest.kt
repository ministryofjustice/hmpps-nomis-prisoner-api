package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.IncentiveBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.latestBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import java.time.LocalDate
import java.time.LocalDateTime

private const val offenderBookingId = 98765L

private val createIncentive: () -> CreateIncentiveRequest = {
  CreateIncentiveRequest(
    iepLevel = "STD",
    comments = "a comment",
    iepDateTime = LocalDateTime.parse("2021-12-01T13:04"),
    prisonId = "WAI",
    userId = "me",
  )
}

class IncentivesResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  @DisplayName("Create")
  @Nested
  inner class CreateIncentive {
    private lateinit var offenderAtMoorlands: Offender

    @BeforeEach
    internal fun createPrisoner() {
      offenderAtMoorlands = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "WAI"),
          ),
      )
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(offenderAtMoorlands)
    }

    @Test
    fun `access forbidden when no authority`() {
      webTestClient.post().uri("/prisoners/booking-id/$offenderBookingId/incentives")
        .body(BodyInserters.fromValue(createIncentive()))
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `access forbidden when no role`() {
      webTestClient.post().uri("/prisoners/booking-id/$offenderBookingId/incentives")
        .headers(setAuthorisation(roles = listOf()))
        .body(BodyInserters.fromValue(createIncentive()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `access forbidden with wrong role`() {
      webTestClient.post().uri("/prisoners/booking-id/$offenderBookingId/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .body(BodyInserters.fromValue(createIncentive()))
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `create with booking not found`() {
      webTestClient.post().uri("/prisoners/booking-id/$offenderBookingId/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .body(BodyInserters.fromValue(createIncentive()))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `will create incentive with correct details`() {
      var offender = repository.lookupOffender("A1234TT")
      var booking = offender?.latestBooking()
      var bookingId = booking?.bookingId

      callCreateEndpoint(bookingId)

      // Spot check that the database has been populated.
      offender = repository.lookupOffender("A1234TT")
      booking = offender?.latestBooking()
      bookingId = booking?.bookingId

      assertThat(booking?.incentives).hasSize(1)
      var incentive = booking?.incentives?.get(0)
      assertThat(incentive?.id?.offenderBooking?.bookingId).isEqualTo(bookingId)
      assertThat(incentive?.id?.sequence).isEqualTo(1)
      assertThat(incentive?.iepLevel).isEqualTo(IEPLevel("STD", "TODO"))

      // Add another to cover the case of existing incentive
      callCreateEndpoint(bookingId)

      offender = repository.lookupOffender("A1234TT")
      booking = offender?.latestBooking()
      bookingId = booking?.bookingId

      assertThat(booking?.incentives).hasSize(2)
      incentive = booking?.incentives?.get(1)
      assertThat(incentive?.id?.offenderBooking?.bookingId).isEqualTo(bookingId)
      assertThat(incentive?.id?.sequence).isEqualTo(2)
      assertThat(incentive?.iepLevel).isEqualTo(IEPLevel("STD", "TODO"))
    }

    private fun callCreateEndpoint(bookingId: Long?) {
      val response = webTestClient.post().uri("/prisoners/booking-id/$bookingId/incentives")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "iepLevel"    : "STD",
            "iepDateTime" : "2021-11-04T13:04",
            "prisonId"    : "WAI",
            "comments"    : "a comment",
            "userId"      : "steve"
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(CreateIncentiveResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.bookingId).isEqualTo(bookingId)
      assertThat(response?.sequence).isGreaterThan(0)
    }
  }

  @DisplayName("filter incentives")
  @Nested
  inner class GetIncentiveIdsByFilterRequest {
    private lateinit var offenderAtMoorlands: Offender
    private lateinit var offenderAtLeeds: Offender
    private lateinit var offenderAtBrixton: Offender

    @BeforeEach
    internal fun createPrisonerWithIEPs() {
      offenderAtMoorlands = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "MDI")
              .withIncentives(
                IncentiveBuilder(iepLevel = "STD", sequence = 1, iepDateTime = LocalDateTime.parse("2022-01-01T12:00")),
                IncentiveBuilder(iepLevel = "ENH", sequence = 2, iepDateTime = LocalDateTime.parse("2022-01-02T12:00")),
              ),
          ),
      )
      offenderAtLeeds = repository.save(
        OffenderBuilder(nomsId = "A4567TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "LEI")
              .withIncentives(
                IncentiveBuilder(iepLevel = "STD", sequence = 1),
                IncentiveBuilder(iepLevel = "ENH", sequence = 2),
              ),
          ),
      )
      offenderAtBrixton = repository.save(
        OffenderBuilder(nomsId = "A7897TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "BXI")
              .withIncentives(
                IncentiveBuilder(iepLevel = "STD", sequence = 1),
                IncentiveBuilder(iepLevel = "ENH", sequence = 2),
              ),
          ),
      )
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(offenderAtMoorlands)
      repository.delete(offenderAtLeeds)
      repository.delete(offenderAtBrixton)
    }

    @Test
    fun `get all incentives ids - no filter specified`() {
      webTestClient.get().uri("/incentives/ids")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(6)
    }

    @Test
    fun `get incentives issued within a given date range`() {
      webTestClient.get().uri {
        it.path("/incentives/ids")
          .queryParam("fromDate", LocalDate.now().minusDays(1).toString())
          .queryParam("toDate", LocalDate.now().toString())
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.numberOfElements").isEqualTo(6)
    }

    @Test
    fun `can request a different page size`() {
      webTestClient.get().uri {
        it.path("/incentives/ids")
          .queryParam("size", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(6)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(0)
        .jsonPath("totalPages").isEqualTo(3)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `can request a different page`() {
      webTestClient.get().uri {
        it.path("/incentives/ids")
          .queryParam("size", "2")
          .queryParam("page", "2")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("totalElements").isEqualTo(6)
        .jsonPath("numberOfElements").isEqualTo(2)
        .jsonPath("number").isEqualTo(2)
        .jsonPath("totalPages").isEqualTo(3)
        .jsonPath("size").isEqualTo(2)
    }

    @Test
    fun `malformed date returns bad request`() {
      webTestClient.get().uri {
        it.path("/incentives/ids")
          .queryParam("fromDate", "202-10-01")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isBadRequest
    }

    @Test
    fun `get incentives prevents access without appropriate role`() {
      assertThat(
        webTestClient.get().uri("/incentives/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get incentives prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/incentives/ids")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @DisplayName("get incentive by id")
  @Nested
  inner class GetIncentiveByIdRequest {
    private lateinit var offenderAtMoorlands: Offender

    @BeforeEach
    internal fun createPrisonerWithIEPs() {
      offenderAtMoorlands = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "MDI")
              .withIncentives(
                IncentiveBuilder(
                  iepLevel = "STD",
                  sequence = 1,
                  iepDateTime = LocalDateTime.parse("2022-01-01T10:00:00"),
                  userId = "JOHN_GEN",
                  auditModuleName = "OIDITRAN",
                ),
                IncentiveBuilder(
                  iepLevel = "ENH",
                  sequence = 2,
                  iepDateTime = LocalDateTime.parse("2022-01-02T10:00:00"),
                  auditModuleName = "OCUWARNG",
                ),
                // earlier date but highest sequence - date takes precedence over sequence for current IEP
                IncentiveBuilder(
                  iepLevel = "BAS",
                  sequence = 3,
                  iepDateTime = LocalDateTime.parse("2020-01-02T10:00:00"),
                ),
              ),
          ),
      )
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(offenderAtMoorlands)
    }

    @Test
    fun `get incentive by id`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("offenderNo").isEqualTo(offenderAtMoorlands.nomsId)
        .jsonPath("bookingId").isEqualTo(bookingId)
        .jsonPath("incentiveSequence").isEqualTo("1")
        .jsonPath("prisonId").isEqualTo("MDI")
        .jsonPath("commentText").isEqualTo("comment")
        .jsonPath("iepLevel.code").isEqualTo("STD")
        .jsonPath("iepLevel.description").isEqualTo("Standard")
        .jsonPath("iepDateTime").isEqualTo("2022-01-01T10:00:00")
        .jsonPath("userId").isEqualTo("JOHN_GEN")
        .jsonPath("currentIep").isEqualTo(false)
        .jsonPath("auditModule").isEqualTo("OIDITRAN")
        .jsonPath("whenCreated").isNotEmpty
        .jsonPath("whenUodated").doesNotExist()
    }

    @Test
    fun `get incentive by id (current)`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("offenderNo").isEqualTo(offenderAtMoorlands.nomsId)
        .jsonPath("bookingId").isEqualTo(bookingId)
        .jsonPath("incentiveSequence").isEqualTo(2)
        .jsonPath("prisonId").isEqualTo("MDI")
        .jsonPath("commentText").isEqualTo("comment")
        .jsonPath("iepLevel.code").isEqualTo("ENH")
        .jsonPath("iepLevel.description").isEqualTo("Enhanced")
        .jsonPath("iepDateTime").isEqualTo("2022-01-02T10:00:00")
        .jsonPath("userId").doesNotExist()
        .jsonPath("currentIep").isEqualTo(true)
        .jsonPath("auditModule").isEqualTo("OCUWARNG")
    }

    @Test
    fun `get incentive by id - not found (sequence)`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/4")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get incentive by id - not found (booking id)`() {
      webTestClient.get().uri("/incentives/booking-id/456/incentive-sequence/1")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get incentive prevents access without appropriate role`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      assertThat(
        webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get incentive prevents access without authorization`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      assertThat(
        webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/1")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @DisplayName("get current incentive for booking")
  @Nested
  inner class GetCurrentIncentiveByBookingRequest {
    private lateinit var offenderAtMoorlands: Offender
    private lateinit var offenderAtMoorlandsWithoutIncentives: Offender

    @BeforeEach
    internal fun createPrisonerWithIEPs() {
      offenderAtMoorlands = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "MDI")
              .withIncentives(
                IncentiveBuilder(
                  iepLevel = "STD",
                  sequence = 1,
                  iepDateTime = LocalDateTime.parse("2022-01-01T10:00:00"),
                  userId = "JOHN_GEN",
                ),
                IncentiveBuilder(
                  iepLevel = "ENH",
                  sequence = 2,
                  iepDateTime = LocalDateTime.parse("2022-01-02T10:00:00"),
                ),
                // earlier date but highest sequence - date takes precedence over sequence for current IEP
                IncentiveBuilder(
                  iepLevel = "BAS",
                  sequence = 3,
                  iepDateTime = LocalDateTime.parse("2020-01-02T10:00:00"),
                ),
              ),
          ),
      )
      offenderAtMoorlandsWithoutIncentives = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "MDI"),
          ),
      )
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(offenderAtMoorlands)
    }

    @Test
    fun `get current incentive`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      webTestClient.get().uri("/incentives/booking-id/$bookingId/current")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("bookingId").isEqualTo(bookingId)
        .jsonPath("incentiveSequence").isEqualTo(2)
        .jsonPath("prisonId").isEqualTo("MDI")
        .jsonPath("commentText").isEqualTo("comment")
        .jsonPath("iepLevel.code").isEqualTo("ENH")
        .jsonPath("iepLevel.description").isEqualTo("Enhanced")
        .jsonPath("iepDateTime").isEqualTo("2022-01-02T10:00:00")
        .jsonPath("userId").doesNotExist()
        .jsonPath("currentIep").isEqualTo(true)
    }

    @Test
    fun `get current incentive by booking - booking not found`() {
      webTestClient.get().uri("/incentives/booking-id/5678/current")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get current incentive by booking - no incentives against booking`() {
      val bookingId = offenderAtMoorlandsWithoutIncentives.latestBooking().bookingId
      webTestClient.get().uri("/incentives/booking-id/$bookingId/current")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get current incentive prevents access without appropriate role`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      assertThat(
        webTestClient.get().uri("/incentives/booking-id/$bookingId/current")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get current incentive prevents access without authorization`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      assertThat(
        webTestClient.get().uri("/incentives/booking-id/$bookingId/current")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @DisplayName("reorder incentive sequences")
  @Nested
  inner class ReorderIncentiveSequences {
    private lateinit var offender: Offender
    private var bookingId = 0L

    @BeforeEach
    internal fun createPrisonerWithIEPs() {
      offender = repository.save(
        OffenderBuilder(nomsId = "A1234TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "MDI")
              .withIncentives(
                IncentiveBuilder(
                  iepLevel = "STD",
                  sequence = 1,
                  iepDateTime = LocalDateTime.parse("2022-01-01T10:00:00"),
                  userId = "JOHN_GEN",
                ),
                IncentiveBuilder(
                  iepLevel = "ENH",
                  sequence = 2,
                  iepDateTime = LocalDateTime.parse("2022-01-02T10:00:00"),
                ),
                IncentiveBuilder(
                  iepLevel = "BAS",
                  sequence = 3,
                  iepDateTime = LocalDateTime.parse("2020-01-02T09:00:00"),
                ),
                IncentiveBuilder(
                  iepLevel = "ENH",
                  sequence = 4,
                  iepDateTime = LocalDateTime.parse("2023-05-31T15:18:28"),
                ),
                IncentiveBuilder(
                  iepLevel = "STD",
                  sequence = 5,
                  iepDateTime = LocalDateTime.parse("2023-05-31T15:25:09"),
                ),
                IncentiveBuilder(
                  iepLevel = "BAS",
                  sequence = 6,
                  iepDateTime = LocalDateTime.parse("2023-05-31T10:25:09"),
                ),
              ),
          ),
      )
      bookingId = offender.latestBooking().bookingId
    }

    @AfterEach
    internal fun deletePrisoner() {
      repository.delete(offender)
    }

    @Nested
    inner class Security {
      @Test
      fun `403 forbidden without appropriate role`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/incentives/reorder")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `401 unauthorised without token`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/incentives/reorder")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `404 not found when bookingId not found`() {
        webTestClient.post().uri("/prisoners/booking-id/999/incentives/reorder")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `200 when reordered successfully`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/incentives/reorder")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk
      }

      @Test
      fun `will reorder the current IEPs on the same date`() {
        webTestClient.get().uri("/incentives/booking-id/$bookingId/current")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("incentiveSequence").isEqualTo(6)
          .jsonPath("iepLevel.code").isEqualTo("BAS")
          .jsonPath("iepDateTime").isEqualTo("2023-05-31T10:25:09")
          .jsonPath("currentIep").isEqualTo(true)

        webTestClient.post().uri("/prisoners/booking-id/$bookingId/incentives/reorder")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/incentives/booking-id/$bookingId/current")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("incentiveSequence").isEqualTo(6)
          .jsonPath("iepLevel.code").isEqualTo("STD")
          .jsonPath("iepDateTime").isEqualTo("2023-05-31T15:25:09")
          .jsonPath("currentIep").isEqualTo(true)

        webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/5")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("incentiveSequence").isEqualTo(5)
          .jsonPath("iepLevel.code").isEqualTo("ENH")
          .jsonPath("iepDateTime").isEqualTo("2023-05-31T15:18:28")
          .jsonPath("currentIep").isEqualTo(false)

        webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/4")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("incentiveSequence").isEqualTo(4)
          .jsonPath("iepLevel.code").isEqualTo("BAS")
          .jsonPath("iepDateTime").isEqualTo("2023-05-31T10:25:09")
          .jsonPath("currentIep").isEqualTo(false)

        webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/3")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("incentiveSequence").isEqualTo(3)
          .jsonPath("iepLevel.code").isEqualTo("BAS")
          .jsonPath("iepDateTime").isEqualTo("2020-01-02T09:00:00")
          .jsonPath("currentIep").isEqualTo(false)

        webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/2")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("incentiveSequence").isEqualTo(2)
          .jsonPath("iepLevel.code").isEqualTo("ENH")
          .jsonPath("iepDateTime").isEqualTo("2022-01-02T10:00:00")
          .jsonPath("currentIep").isEqualTo(false)

        webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("incentiveSequence").isEqualTo(1)
          .jsonPath("iepLevel.code").isEqualTo("STD")
          .jsonPath("iepDateTime").isEqualTo("2022-01-01T10:00:00")
          .jsonPath("currentIep").isEqualTo(false)
      }

      @Test
      fun `will track analytic event when reorder is complete`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/incentives/reorder")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("incentive-resequenced"),
          check {
            assertThat(it["bookingId"]).isEqualTo(bookingId.toString())
            assertThat(it["oldSequence"]).isEqualTo("4, 5, 6")
            assertThat(it["newSequence"]).isEqualTo("6, 4, 5")
          },
          isNull(),
        )
      }

      @Test
      fun `will not track analytic event when reorder does nothing`() {
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/incentives/reorder")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk

        verify(telemetryClient).trackEvent(
          eq("incentive-resequenced"),
          any(),
          isNull(),
        )

        reset(telemetryClient)

        // 2nd reorder should do nothing
        webTestClient.post().uri("/prisoners/booking-id/$bookingId/incentives/reorder")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .exchange()
          .expectStatus().isOk

        verifyNoInteractions(telemetryClient)
      }
    }
  }

  @DisplayName("get global incentive level")
  @Nested
  inner class GetGlobalIncentiveLevel {
    @Test
    fun `get global incentive level`() {
      webTestClient.get().uri("/incentives/reference-codes/STD")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("code").isEqualTo("STD")
        .jsonPath("active").isEqualTo(true)
    }

    @Test
    fun `get global incentive level returns 404 if IEP_LEVEL doesn't exist`() {
      webTestClient.get().uri("/incentives/reference-codes/HHH")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isNotFound
    }

    @Test
    fun `get global incentive prevents access without appropriate role`() {
      assertThat(
        webTestClient.get().uri("/incentives/reference-codes/STD")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `get global incentive prevents access without authorization`() {
      assertThat(
        webTestClient.get().uri("/incentives/reference-codes/STD")
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @DisplayName("create global incentive level")
  @Nested
  inner class CreateGlobalIncentiveLevel {

    @AfterEach
    internal fun deleteIncentiveLevel() {
      webTestClient.delete().uri("/incentives/reference-codes/NIEP")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `create global incentive level`() {
      val response = webTestClient.post().uri("/incentives/reference-codes")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "code"    : "NIEP",
            "description"    : "description for NIEP",
            "active"    : false
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(ReferenceCode::class.java)
        .returnResult().responseBody
      assertThat(response?.code).isEqualTo("NIEP")
      assertThat(response?.domain).isEqualTo("IEP_LEVEL")
      assertThat(response?.description).isEqualTo("description for NIEP")
      assertThat(response?.active).isFalse
      assertThat(response?.sequence).isEqualTo(6)
      assertThat(response?.parentCode).isEqualTo("6")
      assertThat(response?.systemDataFlag).isFalse
    }

    @Test
    fun `create global incentive prevents access without appropriate role`() {
      assertThat(
        webTestClient.post().uri("/incentives/reference-codes")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "code"    : "NIEP",
            "description"    : "description for NIEP",
            "active"    : false
          }""",
            ),
          )
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `create global incentive prevents access without authorization`() {
      assertThat(
        webTestClient.post().uri("/incentives/reference-codes")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "code"    : "NIEP",
            "description"    : "description for NIEP",
            "active"    : false
          }""",
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @DisplayName("update global incentive level")
  @Nested
  inner class UpdateGlobalIncentiveLevel {
    @Test
    fun `update global incentive level`() {
      val response = webTestClient.put().uri("/incentives/reference-codes/EN2")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "description"    : "new description for EN2",
            "active"    : false
          }""",
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody(ReferenceCode::class.java)
        .returnResult().responseBody
      assertThat(response?.code).isEqualTo("EN2")
      assertThat(response?.domain).isEqualTo("IEP_LEVEL")
      assertThat(response?.description).isEqualTo("new description for EN2")
      assertThat(response?.active).isFalse
      assertThat(response?.expiredDate).isToday
      assertThat(response?.systemDataFlag).isFalse
    }

    @Test
    fun `update global incentive prevents access without appropriate role`() {
      assertThat(
        webTestClient.put().uri("/incentives/reference-codes/EN2")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "description"    : "description for EN2",
            "active"    : false
          }""",
            ),
          )
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `update global incentive prevents access without authorization`() {
      assertThat(
        webTestClient.put().uri("/incentives/reference-codes/EN2")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "description"    : "description for EN2",
            "active"    : false
          }""",
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @DisplayName("reorder global incentive levels")
  @Nested
  inner class ReorderGlobalIncentiveLevels {
    @Test
    fun `reorder global incentive levels`() {
      webTestClient.post().uri("/incentives/reference-codes/reorder")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "codeList"    : ["STD","BAS","ENT","EN2","ENH"]
          }""",
          ),
        )
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/incentives/reference-codes/STD")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("sequence").isEqualTo(1)
        .jsonPath("parentCode").isEqualTo("1")

      webTestClient.get().uri("/incentives/reference-codes/ENH")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("sequence").isEqualTo(5)
        .jsonPath("parentCode").isEqualTo("5")
    }

    @Test
    fun `reorder incentive levels ignores missing levels`() {
      webTestClient.post().uri("/incentives/reference-codes/reorder")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "codeList"    : ["FFF","BAS","STD","ENT","EN2","ENH"]
          }""",
          ),
        )
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `reorder global incentive levels prevents access without appropriate role`() {
      assertThat(
        webTestClient.post().uri("/incentives/reference-codes/reorder")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "codeList"    : ["STD","BAS","ENT","EN2","ENH"]
          }""",
            ),
          )
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `reorder global incentive levels prevents access without authorization`() {
      assertThat(
        webTestClient.post().uri("/incentives/reference-codes/reorder")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "codeList"    : ["STD","BAS","ENT","EN2","ENH"]
          }""",
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @DisplayName("create prison incentive level data")
  @Nested
  inner class CreatePrisonIncentiveLevel {
    @BeforeEach
    internal fun setupIncentiveLevel() {
      webTestClient.post().uri("/incentives/reference-codes")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "code"    : "PILD",
            "description"    : "description for PILD",
            "active"    : false
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
    }

    @AfterEach
    internal fun deleteData() {
      webTestClient.delete().uri("/incentives/prison/MDI/code/PILD")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/incentives/reference-codes/PILD")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `create prison incentive level data`() {
      val response = webTestClient.post().uri("/incentives/prison/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "levelCode"    : "PILD",
            "defaultOnAdmission" : false,
            "active"    : true,
            "visitOrderAllowance"    : 3,
            "privilegedVisitOrderAllowance"    : 6,
            "remandTransferLimitInPence"    : 2,
            "remandSpendLimitInPence"    : 7,
            "convictedSpendLimitInPence"    : 9,
            "convictedTransferLimitInPence"    : 1
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(PrisonIncentiveLevelDataResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.iepLevelCode).isEqualTo("PILD")
      assertThat(response?.prisonId).isEqualTo("MDI")
      assertThat(response?.active).isTrue
      assertThat(response?.defaultOnAdmission).isFalse
      assertThat(response?.privilegedVisitOrderAllowance).isEqualTo(6)
      assertThat(response?.visitOrderAllowance).isEqualTo(3)
      assertThat(response?.remandSpendLimitInPence).isEqualTo(7)
      assertThat(response?.remandTransferLimitInPence).isEqualTo(2)
      assertThat(response?.convictedSpendLimitInPence).isEqualTo(9)
      assertThat(response?.convictedTransferLimitInPence).isEqualTo(1)
      assertThat(response?.expiryDate).isNull()
    }

    @Test
    fun `create prison incentive will update if data exists`() {
      webTestClient.post().uri("/incentives/prison/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "levelCode"    : "PILD",
            "defaultOnAdmission" : false,
            "active"    : true,
            "visitOrderAllowance"    : 3,
            "privilegedVisitOrderAllowance"    : 6,
            "remandTransferLimitInPence"    : 2,
            "remandSpendLimitInPence"    : 7,
            "convictedSpendLimitInPence"    : 9,
            "convictedTransferLimitInPence"    : 1
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      // call create again (will update)
      val response = webTestClient.post().uri("/incentives/prison/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "levelCode"    : "PILD",
            "defaultOnAdmission" : false,
            "active"    : true,
            "visitOrderAllowance"    : 10,
            "privilegedVisitOrderAllowance"    : 11,
            "remandTransferLimitInPence"    : 100,
            "remandSpendLimitInPence"    : 101,
            "convictedSpendLimitInPence"    : 400,
            "convictedTransferLimitInPence"    : 401
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody(PrisonIncentiveLevelDataResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.iepLevelCode).isEqualTo("PILD")
      assertThat(response?.prisonId).isEqualTo("MDI")
      assertThat(response?.active).isTrue
      assertThat(response?.defaultOnAdmission).isFalse
      assertThat(response?.privilegedVisitOrderAllowance).isEqualTo(11)
      assertThat(response?.visitOrderAllowance).isEqualTo(10)
      assertThat(response?.remandSpendLimitInPence).isEqualTo(101)
      assertThat(response?.remandTransferLimitInPence).isEqualTo(100)
      assertThat(response?.convictedSpendLimitInPence).isEqualTo(400)
      assertThat(response?.convictedTransferLimitInPence).isEqualTo(401)
      assertThat(response?.expiryDate).isNull()
    }

    @Test
    fun `create prison incentive will reject reject missing request with Incentive level`() {
      assertThat(
        webTestClient.post().uri("/incentives/prison/MDI")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "levelCode"    : "BBB",
            "defaultOnAdmission" : false,
            "active"    : true,
            "visitOrderAllowance"    : 10,
            "privilegedVisitOrderAllowance"    : 11,
            "remandTransferLimitInPence"    : 100,
            "remandSpendLimitInPence"    : 101,
            "convictedSpendLimitInPence"    : 400,
            "convictedTransferLimitInPence"    : 401
          }""",
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Bad request: Incentive level with code=BBB does not exist")
          },
      )
    }

    @Test
    fun `create prison incentive prevents access without appropriate role`() {
      assertThat(
        webTestClient.post().uri("/incentives/prison/MDI")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "levelCode"    : "PILD",
            "defaultOnAdmission" : false,
            "active"    : false,
            "visitOrderAllowance"    : 3,
            "privilegedVisitOrderAllowance"    : 6,
            "remandTransferLimitInPence"    : 2,
            "remandSpendLimitInPence"    : 7,
            "convictedSpendLimitInPence"    : 9,
            "convictedTransferLimitInPence"    : 1
          }""",
            ),
          )
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `create prison incentive prevents access without authorization`() {
      assertThat(
        webTestClient.post().uri("/incentives/prison/MDI")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "levelCode"    : "PILD",
            "defaultOnAdmission" : false,
            "active"    : false,
            "visitOrderAllowance"    : 3,
            "privilegedVisitOrderAllowance"    : 6,
            "remandTransferLimitInPence"    : 2,
            "remandSpendLimitInPence"    : 7,
            "convictedSpendLimitInPence"    : 9,
            "convictedTransferLimitInPence"    : 1
          }""",
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }

  @DisplayName("update prison incentive level data")
  @Nested
  inner class UpdatePrisonIncentiveLevel {
    @BeforeEach
    internal fun setupIncentiveLevel() {
      // creates ABC and DEF global incentive levels with only ABC having prison level incentive data at MDI prison
      webTestClient.post().uri("/incentives/reference-codes")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "code"    : "ABC",
            "description"    : "description for ABC",
            "active"    : true
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/incentives/reference-codes")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "code"    : "DEF",
            "description"    : "description for DEF",
            "active"    : false
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated

      webTestClient.post().uri("/incentives/prison/MDI")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "levelCode"    : "ABC",
            "defaultOnAdmission" : false,
            "active"    : true,
            "visitOrderAllowance"    : 3,
            "privilegedVisitOrderAllowance"    : 6,
            "remandTransferLimitInPence"    : 2,
            "remandSpendLimitInPence"    : 7,
            "convictedSpendLimitInPence"    : 9,
            "convictedTransferLimitInPence"    : 1
          }""",
          ),
        )
        .exchange()
        .expectStatus().isCreated
    }

    @AfterEach
    internal fun deleteData() {
      webTestClient.delete().uri("/incentives/prison/MDI/code/ABC")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/incentives/prison/MDI/code/DEF")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/incentives/reference-codes/ABC")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk

      webTestClient.delete().uri("/incentives/reference-codes/DEF")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `update prison incentive level data`() {
      val response = webTestClient.put().uri("/incentives/prison/MDI/code/ABC")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "defaultOnAdmission" : true,
            "active"    : true,
            "visitOrderAllowance"    : 33,
            "privilegedVisitOrderAllowance"    : 66,
            "remandTransferLimitInPence"    : 22,
            "remandSpendLimitInPence"    : 77,
            "convictedSpendLimitInPence"    : 99,
            "convictedTransferLimitInPence"    : 11
          }""",
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody(PrisonIncentiveLevelDataResponse::class.java)
        .returnResult().responseBody
      assertThat(response?.iepLevelCode).isEqualTo("ABC")
      assertThat(response?.prisonId).isEqualTo("MDI")
      assertThat(response?.active).isTrue
      assertThat(response?.defaultOnAdmission).isTrue
      assertThat(response?.privilegedVisitOrderAllowance).isEqualTo(66)
      assertThat(response?.visitOrderAllowance).isEqualTo(33)
      assertThat(response?.remandSpendLimitInPence).isEqualTo(77)
      assertThat(response?.remandTransferLimitInPence).isEqualTo(22)
      assertThat(response?.convictedSpendLimitInPence).isEqualTo(99)
      assertThat(response?.convictedTransferLimitInPence).isEqualTo(11)
      assertThat(response?.expiryDate).isNull()
    }

    @Test
    fun `update prison incentive request for data that doesn't exist will create`() {
      webTestClient.put().uri("/incentives/prison/MDI/code/DEF")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "defaultOnAdmission" : true,
            "active"    : true,
            "visitOrderAllowance"    : 33,
            "privilegedVisitOrderAllowance"    : 66,
            "remandTransferLimitInPence"    : 22,
            "remandSpendLimitInPence"    : 77,
            "convictedSpendLimitInPence"    : 99,
            "convictedTransferLimitInPence"    : 11
          }""",
          ),
        )
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/incentives/prison/MDI/code/DEF")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("prisonId").isEqualTo("MDI")
        .jsonPath("iepLevelCode").isEqualTo("DEF")
        .jsonPath("defaultOnAdmission").isEqualTo(true)
        .jsonPath("active").isEqualTo(true)
        .jsonPath("expiryDate").doesNotExist()
        .jsonPath("visitOrderAllowance").isEqualTo(33)
        .jsonPath("privilegedVisitOrderAllowance").isEqualTo(66)
        .jsonPath("remandTransferLimitInPence").isEqualTo(22)
        .jsonPath("remandSpendLimitInPence").isEqualTo(77)
        .jsonPath("convictedTransferLimitInPence").isEqualTo(11)
        .jsonPath("convictedSpendLimitInPence").isEqualTo(99)
        .jsonPath("visitAllowanceActive").isEqualTo(true)
        .jsonPath("visitAllowanceExpiryDate").doesNotExist()
    }

    @Test
    fun `update prison incentive to disable the record`() {
      webTestClient.put().uri("/incentives/prison/MDI/code/ABC")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          BodyInserters.fromValue(
            """{
            "defaultOnAdmission" : false,
            "active"    : false,
            "visitOrderAllowance"    : 1,
            "privilegedVisitOrderAllowance"    : 2,
            "remandTransferLimitInPence"    : 550,
            "remandSpendLimitInPence"    : 660,
            "convictedSpendLimitInPence"    : 350,
            "convictedTransferLimitInPence"    : 750
          }""",
          ),
        )
        .exchange()
        .expectStatus().isOk

      webTestClient.get().uri("/incentives/prison/MDI/code/ABC")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("prisonId").isEqualTo("MDI")
        .jsonPath("iepLevelCode").isEqualTo("ABC")
        .jsonPath("active").isEqualTo(false)
        .jsonPath("expiryDate").isEqualTo(LocalDate.now().toString())
        .jsonPath("visitAllowanceActive").isEqualTo(false)
        .jsonPath("visitAllowanceExpiryDate").isEqualTo(LocalDate.now().toString())
    }

    @Test
    fun `update prison will reject reject missing request with Incentive level`() {
      assertThat(
        webTestClient.put().uri("/incentives/prison/MDI/code/BBB")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "defaultOnAdmission" : false,
            "active"    : false,
            "visitOrderAllowance"    : 3,
            "privilegedVisitOrderAllowance"    : 6,
            "remandTransferLimitInPence"    : 2,
            "remandSpendLimitInPence"    : 7,
            "convictedSpendLimitInPence"    : 9,
            "convictedTransferLimitInPence"    : 1
          }""",
            ),
          )
          .exchange()
          .expectStatus().isNotFound
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).contains("Not Found: Incentive level with code=BBB does not exist")
          },
      )
    }

    @Test
    fun `update prison incentive prevents access without appropriate role`() {
      assertThat(
        webTestClient.put().uri("/incentives/prison/MDI/code/ABC")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "defaultOnAdmission" : false,
            "active"    : false,
            "visitOrderAllowance"    : 3,
            "privilegedVisitOrderAllowance"    : 6,
            "remandTransferLimitInPence"    : 2,
            "remandSpendLimitInPence"    : 7,
            "convictedSpendLimitInPence"    : 9,
            "convictedTransferLimitInPence"    : 1
          }""",
            ),
          )
          .exchange()
          .expectStatus().isForbidden,
      )
    }

    @Test
    fun `update prison incentive prevents access without authorization`() {
      assertThat(
        webTestClient.put().uri("/incentives/prison/MDI/code/ABC")
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """{
            "defaultOnAdmission" : false,
            "active"    : false,
            "visitOrderAllowance"    : 3,
            "privilegedVisitOrderAllowance"    : 6,
            "remandTransferLimitInPence"    : 2,
            "remandSpendLimitInPence"    : 7,
            "convictedSpendLimitInPence"    : 9,
            "convictedTransferLimitInPence"    : 1
          }""",
            ),
          )
          .exchange()
          .expectStatus().isUnauthorized,
      )
    }
  }
}
