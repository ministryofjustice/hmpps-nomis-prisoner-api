package uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase

class BookingResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @AfterEach
  fun cleanUp() {
    repository.deleteOffenders()
  }

  @BeforeEach
  fun setup() {
    repository.deleteOffenders()
  }

  @Nested
  inner class GetAllLatestBookingsFromId {
    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/bookings/ids/latest-from-id")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/bookings/ids/latest-from-id")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/bookings/ids/latest-from-id")
          .exchange()
          .expectStatus().isUnauthorized
      }

      @Test
      fun `access allowed for NOMIS_PRISONER_API__SYNCHRONISATION__RW`() {
        webTestClient.get().uri("/bookings/ids/latest-from-id")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }

    @Nested
    inner class HappyPath {

      @BeforeEach
      fun setUp() {
        // create:
        // 10 latest active bookings
        // 10 inactive OUT bookings
        // 10 inactive TRN bookings
        // 30 previous bookings that will always be ignored
        nomisDataBuilder.build {
          (0..9).forEach {
            offender(nomsId = "A123${it}TT") {
              booking()
              booking {
                release()
              }
            }
            offender(nomsId = "A123${it}WW") {
              booking(active = false, agencyLocationId = "OUT")
              booking {
                release()
              }
            }
            offender(nomsId = "A123${it}XX") {
              booking(active = false, agencyLocationId = "TRN")
              booking {
                release()
              }
            }
          }
        }
      }

      @Test
      fun `will find all latest booking with a large page size`() {
        val response: BookingIdsWithLast = webTestClient.get().uri("/bookings/ids/latest-from-id?pageSize=1000")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(response.prisonerIds).hasSize(30)
        assertThat(response.prisonerIds[0].offenderNo).isEqualTo("A1230TT")
        assertThat(response.prisonerIds[29].offenderNo).isEqualTo("A1239XX")
      }

      @Test
      fun `will find all latest active booking with a large page size`() {
        val response: BookingIdsWithLast = webTestClient.get().uri("/bookings/ids/latest-from-id?pageSize=1000&activeOnly=true")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(response.prisonerIds).hasSize(10)
        assertThat(response.prisonerIds[0].offenderNo).isEqualTo("A1230TT")
        assertThat(response.prisonerIds[9].offenderNo).isEqualTo("A1239TT")
      }

      @Test
      fun `will return a page of Ids from first booking`() {
        val response: BookingIdsWithLast = webTestClient.get().uri("/bookings/ids/latest-from-id?pageSize=5&activeOnly=true")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(response.prisonerIds).hasSize(5)
        assertThat(response.prisonerIds[0].offenderNo).isEqualTo("A1230TT")
        assertThat(response.prisonerIds[1].offenderNo).isEqualTo("A1231TT")
        assertThat(response.prisonerIds[2].offenderNo).isEqualTo("A1232TT")
        assertThat(response.prisonerIds[3].offenderNo).isEqualTo("A1233TT")
        assertThat(response.prisonerIds[4].offenderNo).isEqualTo("A1234TT")
      }

      @Test
      fun `can page through results of pages using last bookingId`() {
        val page1: BookingIdsWithLast = webTestClient.get().uri("/bookings/ids/latest-from-id?pageSize=4&activeOnly=true")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(page1.prisonerIds).hasSize(4)
        assertThat(page1.prisonerIds[0].offenderNo).isEqualTo("A1230TT")
        assertThat(page1.prisonerIds[1].offenderNo).isEqualTo("A1231TT")
        assertThat(page1.prisonerIds[2].offenderNo).isEqualTo("A1232TT")
        assertThat(page1.prisonerIds[3].offenderNo).isEqualTo("A1233TT")

        val page2: BookingIdsWithLast = webTestClient.get().uri("/bookings/ids/latest-from-id?pageSize=4&activeOnly=true&bookingId=${page1.prisonerIds.last().bookingId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(page2.prisonerIds).hasSize(4)
        assertThat(page2.prisonerIds[0].offenderNo).isEqualTo("A1234TT")
        assertThat(page2.prisonerIds[1].offenderNo).isEqualTo("A1235TT")
        assertThat(page2.prisonerIds[2].offenderNo).isEqualTo("A1236TT")
        assertThat(page2.prisonerIds[3].offenderNo).isEqualTo("A1237TT")

        val page3: BookingIdsWithLast = webTestClient.get().uri("/bookings/ids/latest-from-id?pageSize=4&activeOnly=true&bookingId=${page2.prisonerIds.last().bookingId}")
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(page3.prisonerIds).hasSize(2)
        assertThat(page3.prisonerIds[0].offenderNo).isEqualTo("A1238TT")
        assertThat(page3.prisonerIds[1].offenderNo).isEqualTo("A1239TT")
      }
    }
  }
}

inline fun <reified B> WebTestClient.ResponseSpec.expectBodyResponse(): B = this.expectStatus().isOk.expectBody(B::class.java).returnResult().responseBody!!
