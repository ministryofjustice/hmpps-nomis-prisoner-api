package uk.gov.justice.digital.hmpps.nomisprisonerapi.resource

import org.assertj.core.api.Assertions
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.IncentiveBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDate
import java.time.LocalTime

class IncentivesResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

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
                IncentiveBuilder(iepLevel = "STD", sequence = 1, iepDate = LocalDate.parse("2022-01-01")),
                IncentiveBuilder(iepLevel = "ENH", sequence = 2, iepDate = LocalDate.parse("2022-01-02"))
              )
          )
      )
      offenderAtLeeds = repository.save(
        OffenderBuilder(nomsId = "A4567TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "LEI")
              .withIncentives(
                IncentiveBuilder(iepLevel = "STD", sequence = 1),
                IncentiveBuilder(iepLevel = "ENH", sequence = 2)
              )
          )
      )
      offenderAtBrixton = repository.save(
        OffenderBuilder(nomsId = "A7897TT")
          .withBooking(
            OffenderBookingBuilder(agencyLocationId = "BXI")
              .withIncentives(
                IncentiveBuilder(iepLevel = "STD", sequence = 1),
                IncentiveBuilder(iepLevel = "ENH", sequence = 2)
              )
          )
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
          .queryParam("fromDate", "2022-01-01")
          .queryParam("toDate", "2022-01-02")
          .build()
      }
        .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_INCENTIVES")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.content..sequence").value(
          Matchers.contains(
            1,
            2
          )
        )
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
      Assertions.assertThat(
        webTestClient.get().uri("/incentives/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden
      )
    }

    @Test
    fun `get incentives prevents access without authorization`() {
      Assertions.assertThat(
        webTestClient.get().uri("/incentives/ids")
          .exchange()
          .expectStatus().isUnauthorized
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
                  iepDate = LocalDate.parse("2022-01-01"),
                  iepTime = LocalTime.parse("10:00:00")
                ),
                IncentiveBuilder(
                  iepLevel = "ENH",
                  sequence = 2,
                  iepDate = LocalDate.parse("2022-01-02"),
                  iepTime = LocalTime.parse("10:00:00")
                )
              )
          )
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
        .jsonPath("bookingId").isEqualTo(bookingId)
        .jsonPath("incentiveSequence").isEqualTo("1")
        .jsonPath("prisonId").isEqualTo("MDI")
        .jsonPath("commentText").isEqualTo("comment")
        .jsonPath("iepLevel.code").isEqualTo("STD")
        .jsonPath("iepLevel.description").isEqualTo("Standard")
        .jsonPath("iepDateTime").isEqualTo("2022-01-01T10:00:00")
    }

    @Test
    fun `get incentive by id - not found (sequence)`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/3")
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
      Assertions.assertThat(
        webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/1")
          .headers(setAuthorisation(roles = listOf("ROLE_BLA")))
          .exchange()
          .expectStatus().isForbidden
      )
    }

    @Test
    fun `get incentive prevents access without authorization`() {
      val bookingId = offenderAtMoorlands.latestBooking().bookingId
      Assertions.assertThat(
        webTestClient.get().uri("/incentives/booking-id/$bookingId/incentive-sequence/1")
          .exchange()
          .expectStatus().isUnauthorized
      )
    }
  }
}

private fun Offender.latestBooking(): OffenderBooking =
  this.bookings.firstOrNull { it.active } ?: throw IllegalStateException("Offender has no active bookings")
