package uk.gov.justice.digital.hmpps.nomisprisonerapi.visits

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyInternalLocation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AgencyVisitSlot
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.WeekDay
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyInternalLocationRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitDayRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.AgencyVisitTimeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.VisitRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.prisoners.expectBodyResponse
import java.time.LocalDate
import java.time.LocalTime

class OfficialVisitsResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var visitRepository: VisitRepository

  @Autowired
  private lateinit var offenderRepository: OffenderRepository

  @Autowired
  private lateinit var personRepository: PersonRepository

  @Autowired
  private lateinit var agencyInternalLocationRepository: AgencyInternalLocationRepository

  @Autowired
  private lateinit var agencyVisitDayRepository: AgencyVisitDayRepository

  @Autowired
  private lateinit var agencyVisitTimeRepository: AgencyVisitTimeRepository

  @DisplayName("GET /official-visits/ids")
  @Nested
  inner class GetOfficialVisitIds {
    lateinit var visitIds: MutableList<Long>

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        val visitor = person { }

        offender(nomsId = "A1234TT") {
          booking {
            visitBalance { }
            contact(person = visitor)
            visitIds = (1..30).map { visit(visitTypeCode = "OFFI", startDateTimeString = "2023-01-01T10:00:00").id }.toMutableList()
            visit(visitTypeCode = "SCON")
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      visitRepository.deleteAll()
      offenderRepository.deleteAll()
      personRepository.deleteAll()
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/official-visits/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/official-visits/ids")
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/official-visits/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `by default will return first 20 visits`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(30)
          .jsonPath("content.size()").isEqualTo(20)
          .jsonPath("page.number").isEqualTo(0)
          .jsonPath("page.totalPages").isEqualTo(2)
          .jsonPath("page.size").isEqualTo(20)
      }

      @Test
      fun `can set page size`() {
        webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("size", "1")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("page.totalElements").isEqualTo(30)
          .jsonPath("content.size()").isEqualTo(1)
          .jsonPath("page.number").isEqualTo(0)
          .jsonPath("page.totalPages").isEqualTo(30)
          .jsonPath("page.size").isEqualTo(1)
      }

      @Test
      fun `id just contains visit id`() {
        val pageResponse: VisitIdPageResponse = webTestClient.get().uri {
          it.path("/official-visits/ids")
            .queryParam("size", "2")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectBodyResponse()

        assertThat(pageResponse.content).hasSize(2)
        assertThat(pageResponse.content[0].visitId).isEqualTo(visitIds[0])
        assertThat(pageResponse.content[1].visitId).isEqualTo(visitIds[1])
      }
    }
  }

  @DisplayName("GET /official-visits/{visitId}")
  @Nested
  inner class GetOfficialVisit {
    var officialVisitId = 0L
    var socialVisitId = 0L
    lateinit var visitSlot: AgencyVisitSlot
    lateinit var room: AgencyInternalLocation

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        room = agencyInternalLocation(
          locationCode = "BXI-VISIT-1",
          locationType = "VISIT",
          prisonId = "BXI",
        )
        agencyVisitDay(prisonerId = "BXI", weekDay = WeekDay.MON) {
          visitTimeSlot(
            timeSlotSequence = 1,
            startTime = LocalTime.parse("10:00"),
            endTime = LocalTime.parse("11:00"),
            effectiveDate = LocalDate.parse("2023-01-01"),
            expiryDate = LocalDate.parse("2033-01-31"),
          ) {
            visitSlot = visitSlot(
              agencyInternalLocation = room,
              maxGroups = null,
              maxAdults = null,
            )
          }
        }

        val visitor = person { }

        offender(nomsId = "A1234TT") {
          booking {
            visitBalance { }
            contact(person = visitor)
            officialVisitId = visit(
              visitTypeCode = "OFFI",
              startDateTimeString = "2023-01-01T10:00:00",
              visitSlot = visitSlot,
            ).id
            socialVisitId = visit(visitTypeCode = "SCON").id
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      visitRepository.deleteAll()
      offenderRepository.deleteAll()
      personRepository.deleteAll()
      agencyVisitTimeRepository.deleteAll()
      agencyVisitDayRepository.deleteAll()
      agencyInternalLocationRepository.delete(room)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 400 bad request if visit is a social visit`() {
        webTestClient.get().uri("/official-visits/{visitId}", socialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `will return 404 if visit does not exist`() {
        webTestClient.get().uri("/official-visits/{visitId}", 9999)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return the visit`() {
        webTestClient.get().uri("/official-visits/{visitId}", officialVisitId)
          .headers(setAuthorisation(roles = listOf("NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
      }
    }
  }
}

private data class VisitIdPageResponse(
  val content: List<VisitIdResponse>,
)
