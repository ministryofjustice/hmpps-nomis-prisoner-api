package uk.gov.justice.digital.hmpps.nomisprisonerapi.incentives

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
    lateinit var offenderAtMoorlands: Offender

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
    lateinit var offenderAtMoorlands: Offender
    lateinit var offenderAtLeeds: Offender
    lateinit var offenderAtBrixton: Offender

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
    lateinit var offenderAtMoorlands: Offender

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
    lateinit var offenderAtMoorlands: Offender
    lateinit var offenderAtMoorlandsWithoutIncentives: Offender

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

  @DisplayName("get global incentive level")
  @Nested
  inner class getGlobalIncentiveLevel {
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
  inner class createGlobalIncentiveLevel {

    @AfterEach
    internal fun deleteIncentiveLevel() {
      webTestClient.delete().uri("/incentives/reference-codes/NIEP")
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
    }

    @Test
    fun `create global incxvcxvcentive level`() {
      assertThat("NIEP").isEqualTo("NIEP")
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
            "domain" : "IEP_LEVEL",
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
      assertThat(response?.active).isFalse()
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
            "domain" : "IEP_LEVEL",
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
            "domain" : "IEP_LEVEL",
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
  inner class updateGlobalIncentiveLevel {
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
      assertThat(response?.active).isFalse()
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
  inner class reorderGlobalIncentiveLevels {
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
        .expectBodyList(ReferenceCode::class.java)
        .returnResult().responseBody
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
        .expectBodyList(ReferenceCode::class.java)
        .returnResult()
        .responseBody
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
}
