package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking

class PrisonersResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Nested
  @DisplayName("GET /prisoners/ids")
  inner class GetPrisoners {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      lateinit var activePrisoner1: Offender
      lateinit var activePrisoner2: Offender
      lateinit var inactivePrisoner1: Offender

      @BeforeEach
      internal fun createPrisoners() {
        deletePrisoners()

        nomisDataBuilder.build {
          activePrisoner1 =
            offender(nomsId = "A1234TT") {
              booking {}
            }
          activePrisoner2 = offender(nomsId = "A1234SS") {
            booking {}
          }

          inactivePrisoner1 = offender(nomsId = "A1234WW") {
            booking(active = false)
          }
        }
      }

      @AfterEach
      fun deletePrisoners() {
        repository.deleteOffenders()
      }

      @Test
      fun `will return count of all active prisoners by default`() {
        webTestClient.get().uri("/prisoners/ids?size=1&page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.totalElements").isEqualTo(2)
          .jsonPath("$.numberOfElements").isEqualTo(1)
      }

      @Test
      fun `will return a page of prisoners ordered by booking id ASC`() {
        webTestClient.get().uri("/prisoners/ids?page=0")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.totalElements").isEqualTo(2)
          .jsonPath("$.numberOfElements").isEqualTo(2)
          .jsonPath("$.content[0].bookingId").isNumber
          .jsonPath("$.content[0].offenderNo").isEqualTo("A1234TT")
          .jsonPath("$.content[1].bookingId").isNumber
          .jsonPath("$.content[1].offenderNo").isEqualTo("A1234SS")
      }

      @Test
      fun `finding all prisoners (including inactive ones) is not supported currently`() {
        webTestClient.get().uri("/prisoners/ids?size=1&page=0&active=false")
          .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
          .exchange()
          .expectStatus().is5xxServerError
      }
    }
  }

  @Nested
  inner class GetPrisonerBookings {
    private lateinit var bookingBxi: OffenderBooking
    private lateinit var bookingOut: OffenderBooking
    private lateinit var bookingTrn: OffenderBooking

    @BeforeEach
    internal fun createPrisoners() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234TT") {
          bookingBxi = booking(agencyLocationId = "BXI")
        }
        offender(nomsId = "A1234WW") {
          bookingOut = booking(active = false, agencyLocationId = "OUT")
        }
        offender(nomsId = "A1234XX") {
          bookingTrn = booking(active = false, agencyLocationId = "TRN")
        }
      }
    }

    @AfterEach
    fun deletePrisoners() {
      repository.deleteOffenders()
    }

    @Test
    fun `should return unauthorised with no auth token`() {
      webTestClient.post().uri("/prisoners/bookings")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("[]")
        .exchange()
        .expectStatus().isUnauthorized
    }

    @Test
    fun `should return forbidden when no role`() {
      webTestClient.post().uri("/prisoners/bookings")
        .headers(setAuthorisation(roles = listOf()))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("[]")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return forbidden with wrong role`() {
      webTestClient.post().uri("/prisoners/bookings")
        .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("[]")
        .exchange()
        .expectStatus().isForbidden
    }

    @Test
    fun `should return empty list if not found`() {
      webTestClient.post().uri("/prisoners/bookings")
        .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(""" [99999] """))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(0)
    }

    @Test
    fun `should get prisoner details`() {
      webTestClient.post().uri("/prisoners/bookings")
        .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(""" [${bookingBxi.bookingId}] """))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(1)
        .jsonPath("$[0].location").isEqualTo("BXI")
        .jsonPath("$[0].bookingId").isEqualTo(bookingBxi.bookingId)
        .jsonPath("$[0].offenderNo").isEqualTo("A1234TT")
    }

    @Test
    fun `should get inactive prisoner details`() {
      webTestClient.post().uri("/prisoners/bookings")
        .headers(setAuthorisation(roles = listOf("ROLE_SYNCHRONISATION_REPORTING")))
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(""" [${bookingOut.bookingId}, ${bookingTrn.bookingId}] """))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.size()").isEqualTo(2)
        .jsonPath("$[0].location").isEqualTo("OUT")
        .jsonPath("$[0].bookingId").isEqualTo(bookingOut.bookingId)
        .jsonPath("$[0].offenderNo").isEqualTo("A1234WW")
        .jsonPath("$[1].location").isEqualTo("TRN")
        .jsonPath("$[1].bookingId").isEqualTo(bookingTrn.bookingId)
        .jsonPath("$[1].offenderNo").isEqualTo("A1234XX")
    }
  }
}
