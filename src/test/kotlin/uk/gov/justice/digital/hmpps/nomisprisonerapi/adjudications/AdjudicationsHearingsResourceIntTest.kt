package uk.gov.justice.digital.hmpps.nomisprisonerapi.adjudications

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.BodyInserters
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationIncident
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

class AdjudicationsHearingsResourceIntTest : IntegrationTestBase() {
  @Autowired
  lateinit var repository: Repository
  private var aLocationInMoorland = -41L
  private var aSecondLocationInMoorland = -42L

  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @BeforeEach
  fun setUp() {
    aLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-001", "MDI").locationId
    aSecondLocationInMoorland = repository.getInternalLocationByDescription("MDI-1-1-002", "MDI").locationId
  }

  @DisplayName("POST /adjudications/adjudication-number/{adjudicationNumber}/hearings")
  @Nested
  inner class CreateHearing {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L

    @BeforeEach
    fun createPrisonerAndAdjudication() {
      nomisDataBuilder.build {
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {
              charge(offenceCode = "51:1B", generateOfficeId = false)
              charge(offenceCode = "51:1A", generateOfficeId = false)
            }
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteHearingByAdjudicationNumber(existingAdjudicationNumber)
      repository.delete(existingIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearing()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearing()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearing()))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private lateinit var prisonerWithNoBookings: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          prisonerWithNoBookings = offender(nomsId = "A9876AK")
        }
      }

      @Test
      fun `will return 404 if adjudication not found`() {
        webTestClient.post().uri("/adjudications/adjudication-number/88888/hearings")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearing()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will return 400 if hearing type is not valid`() {
        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearing(hearingType = "VVV")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("""Hearing type VVV not found""")
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `create an adjudication hearing`() {
        val hearingResponse =
          webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
            .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
            .contentType(MediaType.APPLICATION_JSON)
            .body(
              BodyInserters.fromValue(
                aHearing(),
              ),
            )
            .exchange()
            .expectStatus().isOk
            .expectBody(CreateHearingResponse::class.java)
            .returnResult().responseBody!!
        assertThat(hearingResponse.hearingId).isNotNull

        verify(telemetryClient).trackEvent(
          eq("hearing-created"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("hearingId", hearingResponse.hearingId.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
          },
          isNull(),
        )
      }

      @Test
      fun `will set offenceId on charge when missing`() {
        repository.runInTransaction {
          with(repository.adjudicationIncidentPartyRepository.findByAdjudicationNumber(existingAdjudicationNumber)!!) {
            assertThat(charges[0].offenceId).isNull()
            assertThat(charges[1].offenceId).isNull()
          }
        }

        webTestClient.post().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearing(),
            ),
          )
          .exchange()
          .expectStatus().isOk

        repository.runInTransaction {
          with(repository.adjudicationIncidentPartyRepository.findByAdjudicationNumber(existingAdjudicationNumber)!!) {
            assertThat(charges[0].offenceId).isEqualTo("$existingAdjudicationNumber/1")
            assertThat(charges[1].offenceId).isEqualTo("$existingAdjudicationNumber/2")
          }
        }
      }
    }

    private fun aHearing(
      hearingType: String = "GOV",
      internalLocationId: Long = aLocationInMoorland,
      agencyId: String = "MDI",
      hearingDate: String = "2023-01-01",
      hearingTime: String = "10:15",
    ): String =
      """
      {
        "hearingType": "$hearingType",
        "hearingDate": "$hearingDate",
        "hearingTime": "$hearingTime",
        "internalLocationId": $internalLocationId,
        "agencyId": "$agencyId"
      }
      """.trimIndent()
  }

  @DisplayName("PUT /adjudications/adjudication-number/{adjudicationNumber}/hearings/hearingId")
  @Nested
  inner class UpdateHearing {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private lateinit var existingHearing: AdjudicationHearing
    private val existingAdjudicationNumber = 123456L

    @BeforeEach
    fun createPrisonerAndAdjudication() {
      nomisDataBuilder.build {
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {
              charge(offenceCode = "51:1B")
              existingHearing = hearing(
                internalLocationId = aLocationInMoorland,
                scheduleDate = LocalDate.parse("2023-01-02"),
                scheduleTime = LocalDateTime.parse("2023-01-02T14:00:00"),
                hearingDate = LocalDate.parse("2023-01-03"),
                hearingTime = LocalDateTime.parse("2023-01-03T15:00:00"),
                hearingStaff = reportingStaff,
                hearingTypeCode = AdjudicationHearingType.GOVERNORS_HEARING,
              )
            }
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteHearingByAdjudicationNumber(existingAdjudicationNumber)
      repository.delete(existingIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingUpdate()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingUpdate()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}")
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingUpdate()))
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private lateinit var prisonerWithNoBookings: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          prisonerWithNoBookings = offender(nomsId = "A9876AK")
        }
      }

      @Test
      fun `will return 404 if adjudication not found`() {
        webTestClient.put().uri("/adjudications/adjudication-number/88888/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingUpdate()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will return 404 if hearing not found`() {
        webTestClient.put().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/88888")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingUpdate()))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Adjudication hearing with hearing Id 88888 not found")
      }

      @Test
      fun `will return 400 if hearing type is not valid`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(BodyInserters.fromValue(aHearingUpdate(hearingType = "VVV")))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("""Hearing type VVV not found""")
      }

      @Test
      fun `will return 400 if update fields not provided`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              """
      {
      }
      """,
            ),
          )
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `update an adjudication hearing`() {
        webTestClient.put()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .body(
            BodyInserters.fromValue(
              aHearingUpdate(
                hearingDate = "2023-06-06",
                hearingTime = "14:30",
                internalLocationId = aSecondLocationInMoorland,
              ),
            ),
          )
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("hearingDate").isEqualTo("2023-06-06")
          .jsonPath("hearingTime").isEqualTo("14:30:00")
          .jsonPath("internalLocation.description").isEqualTo("MDI-1-1-002")

        verify(telemetryClient).trackEvent(
          eq("hearing-updated"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("hearingId", existingHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
          },
          isNull(),
        )
      }
    }

    private fun aHearingUpdate(
      hearingType: String = "GOV",
      internalLocationId: Long = aLocationInMoorland,
      hearingDate: String = "2023-01-01",
      hearingTime: String = "10:15",
    ): String =
      """
      {
        "hearingType": "$hearingType",
        "hearingDate": "$hearingDate",
        "hearingTime": "$hearingTime",
        "internalLocationId": $internalLocationId
      }
      """.trimIndent()
  }

  @DisplayName("GET /adjudications/hearings/{hearingId}")
  @Nested
  inner class GetHearing {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private val existingAdjudicationNumber = 123456L
    private lateinit var existingHearing: AdjudicationHearing

    @BeforeEach
    fun createPrisonerAndAdjudication() {
      nomisDataBuilder.build {
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {
              charge(offenceCode = "51:1B")
              existingHearing = hearing(
                internalLocationId = aLocationInMoorland,
                scheduleDate = LocalDate.parse("2023-01-02"),
                scheduleTime = LocalDateTime.parse("2023-01-02T14:00:00"),
                hearingDate = LocalDate.parse("2023-01-03"),
                hearingTime = LocalDateTime.parse("2023-01-03T15:00:00"),
                hearingStaff = reportingStaff,
              )
            }
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteHearingByAdjudicationNumber(existingAdjudicationNumber)
      repository.delete(existingIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/adjudications/hearings/123")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/adjudications/hearings/123")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/adjudications/hearings/123")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `will return 404 if hearing not found`() {
        webTestClient.get().uri("/adjudications/hearings/123")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Hearing not found. Hearing Id: 123")
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `get adjudication hearing`() {
        webTestClient.get().uri("/adjudications/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("type.code").isEqualTo("GOV")
          .jsonPath("type.description").isEqualTo("Governor's Hearing")
          .jsonPath("scheduleDate").isEqualTo("2023-01-02")
          .jsonPath("scheduleTime").isEqualTo("14:00:00")
          .jsonPath("hearingDate").isEqualTo("2023-01-03")
          .jsonPath("hearingTime").isEqualTo("15:00:00")
          .jsonPath("comment").isEqualTo("Hearing comment")
          .jsonPath("representativeText").isEqualTo("rep text")
          .jsonPath("hearingStaff.staffId").isEqualTo(reportingStaff.id)
          .jsonPath("hearingStaff.username").isEqualTo("JANESTAFF")
          .jsonPath("representativeText").isEqualTo("rep text")
          .jsonPath("internalLocation.description").isEqualTo("MDI-1-1-001")
          .jsonPath("eventStatus.code").isEqualTo("SCH")
          .jsonPath("eventId").isEqualTo(1)
          .jsonPath("createdByUsername").isNotEmpty
          .jsonPath("createdDateTime").isNotEmpty
      }
    }
  }

  @DisplayName("DELETE /adjudications/adjudication-number/{adjudicationNumber}/hearings/hearingId")
  @Nested
  inner class DeleteHearing {
    private val offenderNo = "A1965NM"
    private lateinit var prisoner: Offender
    private lateinit var reportingStaff: Staff
    private lateinit var existingIncident: AdjudicationIncident
    private lateinit var existingHearing: AdjudicationHearing
    private val existingAdjudicationNumber = 123456L

    @BeforeEach
    fun createPrisonerAndAdjudication() {
      nomisDataBuilder.build {
        reportingStaff = staff(firstName = "JANE", lastName = "STAFF") {
          account(username = "JANESTAFF")
        }
        existingIncident = adjudicationIncident(reportingStaff = reportingStaff) {}
        prisoner = offender(nomsId = offenderNo) {
          booking {
            adjudicationParty(incident = existingIncident, adjudicationNumber = existingAdjudicationNumber) {
              val previousCharge = charge(offenceCode = "51:1B")
              existingHearing = hearing(
                internalLocationId = aLocationInMoorland,
                scheduleDate = LocalDate.parse("2023-01-02"),
                scheduleTime = LocalDateTime.parse("2023-01-02T14:00:00"),
                hearingDate = LocalDate.parse("2023-01-03"),
                hearingTime = LocalDateTime.parse("2023-01-03T15:00:00"),
                hearingStaff = reportingStaff,
                hearingTypeCode = AdjudicationHearingType.GOVERNORS_HEARING,
              ) {
                result(
                  charge = previousCharge,
                  pleaFindingCode = "NOT_GUILTY",
                  findingCode = "PROVED",
                ) {
                  award(
                    statusCode = "SUSPENDED",
                    sanctionCode = "ADA",
                    effectiveDate = LocalDate.parse("2023-01-08"),
                    statusDate = LocalDate.parse("2023-01-09"),
                    sanctionDays = 4,
                    sanctionIndex = 1,
                  )
                }
                notification(
                  staff = reportingStaff,
                  deliveryDate = LocalDate.parse("2023-01-04"),
                  deliveryDateTime = LocalDateTime.parse("2023-01-04T10:00:00"),
                )
              }
            }
          }
        }
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteHearingByAdjudicationNumber(existingAdjudicationNumber)
      repository.delete(existingIncident)
      repository.delete(prisoner)
      repository.delete(reportingStaff)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private lateinit var prisonerWithNoBookings: Offender

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          prisonerWithNoBookings = offender(nomsId = "A9876AK")
        }
      }

      @Test
      fun `will return 404 if adjudication not found`() {
        webTestClient.delete().uri("/adjudications/adjudication-number/88888/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage")
          .isEqualTo("Hearing with id ${existingHearing.id} delete failed: Adjudication party with adjudication number 88888 not found")
      }

      @Test
      fun `will not throw error and will track event if hearing not found`() {
        webTestClient.delete().uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/88888")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk()

        verify(telemetryClient).trackEvent(
          eq("hearing-delete-not-found"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("hearingId", "88888")
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
          },
          isNull(),
        )
      }
    }

    @Nested
    inner class HappyPath {

      @Test
      fun `delete an adjudication hearing`() {
        webTestClient.delete()
          .uri("/adjudications/adjudication-number/$existingAdjudicationNumber/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk

        webTestClient.get().uri("/adjudications/hearings/${existingHearing.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
          .expectBody()
          .jsonPath("developerMessage").isEqualTo("Hearing not found. Hearing Id: ${existingHearing.id}")

        verify(telemetryClient).trackEvent(
          eq("hearing-deleted"),
          org.mockito.kotlin.check {
            assertThat(it).containsEntry("hearingId", existingHearing.id.toString())
            assertThat(it).containsEntry("adjudicationNumber", existingAdjudicationNumber.toString())
          },
          isNull(),
        )
      }
    }
  }
}
