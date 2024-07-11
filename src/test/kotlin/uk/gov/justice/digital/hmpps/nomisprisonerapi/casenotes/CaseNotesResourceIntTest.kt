package uk.gov.justice.digital.hmpps.nomisprisonerapi.casenotes

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.WebTestClient.RequestHeadersSpec
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCaseNoteRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CaseNotesResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderCaseNoteRepository: OffenderCaseNoteRepository

  @Autowired
  private lateinit var repository: Repository

  private lateinit var prisoner: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var staff1: Staff
  private lateinit var casenote1: OffenderCaseNote

  private val now = LocalDateTime.now()

  @AfterEach
  fun tearDown() {
    repository.deleteCaseNotes()
    repository.deleteOffenders()
    repository.deleteStaff()
  }

  @DisplayName("GET /casenotes/{caseNoteId}")
  @Nested
  inner class GetCaseNote {
    var bookingId = 0L

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/casenotes/99")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/casenotes/99")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/casenotes/99")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when case note id not found`() {
        webTestClient.get().uri("/casenotes/99")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {

      @BeforeEach
      fun setUp() {
        nomisDataBuilder.build {
          staff1 = staff(firstName = "JANE", lastName = "NARK") {
            account(username = "JANE.NARK")
          }
          offender(nomsId = "A1234AB") {
            bookingId = booking {
              casenote1 = caseNote(
                caseNoteType = "ALL",
                caseNoteSubType = "SA",
                author = staff1,
                caseNoteText = "A note",
              )
            }.bookingId
          }
        }
      }

      @Test
      fun `returned data for a caseNote`() {
        webTestClient.get().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("caseNoteId").isEqualTo(casenote1.id)
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("caseNoteType.code").isEqualTo("ALL")
          .jsonPath("caseNoteSubType.code").isEqualTo("SA")
          .jsonPath("authorUsername").isEqualTo("JANE.NARK")
          .jsonPath("prisonId").isEqualTo("BXI")
          .jsonPath("caseNoteText").isEqualTo("A note")
          .jsonPath("amended").isEqualTo("false")
          .jsonPath("occurrenceDateTime")
          .value<String> { assertThat(LocalDateTime.parse(it)).isCloseTo(now, within(2, ChronoUnit.MINUTES)) }
          .jsonPath("auditModuleName").isEqualTo("A_MODULE")
      }
    }
  }

  @DisplayName("POST /prisoners/{offenderNo}/casenotes")
  @Nested
  inner class CreateCaseNote {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff1 = staff(firstName = "JANE", lastName = "NARK") {
          account(username = "JANE.NARK")
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking = booking {
          }
        }
      }
    }

    @Nested
    inner class Security {
      private val validCaseNote = CreateCaseNoteRequest(
        caseNoteType = "ALL",
        caseNoteSubType = "SA",
        occurrenceDateTime = LocalDateTime.now(),
        authorUsername = "JANE.PEEL",
        caseNoteText = "the contents",
      )

      @Test
      fun `access forbidden when no role`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      private val validCaseNote = CreateCaseNoteRequest(
        caseNoteType = "ALL",
        caseNoteSubType = "SEN",
        occurrenceDateTime = LocalDateTime.now(),
        authorUsername = "JANE.NARK",
        caseNoteText = "the contents",
      )

      @Test
      fun `validation fails when prisoner does not exist`() {
        webTestClient.post().uri("/prisoners/A9999ZZ/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `validation fails when type is not valid`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteType = "NNNNN"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when subtype is not valid`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteSubType = "NNNNN"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when subtype is not a child of type`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteType = "ACP"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when author does not exist`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(authorUsername = "another"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when note is too long`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteText = "a".repeat(4001)))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `creating a case note will allow the data to be retrieved`() {
        val response = webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "caseNoteType": "ALL",
                 "caseNoteSubType": "SEN",
                 "occurrenceDateTime": "2024-06-01T15:00",
                 "authorUsername": "JANE.NARK",
                 "caseNoteText": "the contents"
              }
            
            """.trimIndent(),
          )
          .exchange()
          .expectStatus().isEqualTo(201)
          .expectBody(CreateCaseNoteResponse::class.java)
          .returnResult()
          .responseBody!!

        assertThat(response.id).isGreaterThan(0)

        repository.runInTransaction {
          val newCaseNote = offenderCaseNoteRepository.findByIdOrNull(response.id)!!

          assertThat(newCaseNote.id).isEqualTo(response.id)
          assertThat(newCaseNote.offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(newCaseNote.occurrenceDate).isEqualTo(LocalDate.parse("2024-06-01"))
          assertThat(newCaseNote.occurrenceDateTime).isEqualTo(LocalDateTime.parse("2024-06-01T15:00"))
          assertThat(newCaseNote.caseNoteType.code).isEqualTo("ALL")
          assertThat(newCaseNote.caseNoteSubType.code).isEqualTo("SEN")
          assertThat(newCaseNote.author.lastName).isEqualTo("NARK")
          assertThat(newCaseNote.agencyLocation?.id).isEqualTo("BXI")
          assertThat(newCaseNote.caseNoteText).isEqualTo("the contents")
          assertThat(newCaseNote.amendmentFlag).isFalse()
          assertThat(newCaseNote.noteSourceCode).isNull()
          assertThat(newCaseNote.dateCreation).isEqualTo(newCaseNote.occurrenceDate)
          assertThat(newCaseNote.timeCreation).isEqualTo(newCaseNote.occurrenceDateTime)

          repository.delete(newCaseNote)
        }
      }
    }
  }

  @DisplayName("PUT /casenotes/{caseNoteId}")
  @Nested
  inner class AmendCaseNote {
    private val validCaseNote = AmendCaseNoteRequest(
      caseNoteType = "ALL",
      caseNoteSubType = "SA",
      occurrenceDateTime = now,
      authorUsername = "JANE.PEEL",
      caseNoteText = "An amended note",
    )

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff1 = staff(firstName = "JANE", lastName = "NARK") {
          account(username = "JANE.NARK")
        }
        staff(firstName = "JANE", lastName = "PEEL") {
          account(username = "JANE.PEEL")
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking {
            casenote1 = caseNote(
              caseNoteType = "ALERT",
              caseNoteSubType = "ACTIVE",
              author = staff1,
              caseNoteText = "A note",
            )
          }
        }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.put().uri("/casenotes/1")
          .headers(setAuthorisation(roles = listOf()))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.put().uri("/casenotes/1")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.put().uri("/casenotes/1")
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `validation fails when case note does not exist`() {
        webTestClient.put().uri("/casenotes/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `validation fails when type is not valid`() {
        webTestClient.put().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteType = "NNNNN"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when subtype is not valid`() {
        webTestClient.put().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteSubType = "NNNNN"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when subtype is not a child of type`() {
        webTestClient.put().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteType = "ACP"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when author does not exist`() {
        webTestClient.put().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(authorUsername = "another"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when note is too long`() {
        webTestClient.put().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteText = "a".repeat(4001)))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `Can update the caseNote and retrieve those caseNote updates`() {
        webTestClient.put().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isEqualTo(200)

        repository.runInTransaction {
          val newCaseNote = offenderCaseNoteRepository.findByIdOrNull(casenote1.id)!!

          assertThat(newCaseNote.occurrenceDate).isEqualTo(now.toLocalDate())
          assertThat(newCaseNote.occurrenceDateTime).isCloseTo(now, within(2, ChronoUnit.MINUTES))
//          assertThat(newCaseNote.caseNoteType.code).isEqualTo("SA")
//          assertThat(newCaseNote.caseNoteSubType.code).isEqualTo("TB")
//          assertThat(newCaseNote.author.lastName).isEqualTo("PEEL")
          // TODO not clear yet what fields it is valid to update
          assertThat(newCaseNote.caseNoteText).isEqualTo("An amended note")
          assertThat(newCaseNote.dateCreation).isEqualTo(newCaseNote.occurrenceDate)
          assertThat(newCaseNote.timeCreation).isEqualTo(newCaseNote.occurrenceDateTime)
        }
      }
    }
  }

  @DisplayName("GET /bookings/{bookingId}/casenotes")
  @Nested
  @Disabled
  inner class GetCaseNotesByBookingId {
    private var bookingNoCaseNotesId = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff1 = staff(firstName = "JANE", lastName = "NARK") {
          account(username = "JANE.NARK")
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking = booking {
            casenote1 = caseNote(
              caseNoteType = "ACP",
              caseNoteSubType = "POEM",
              author = staff1,
              caseNoteText = "Note 1",
            )
            caseNote(
              caseNoteType = "ACP",
              caseNoteSubType = "POPEM",
              author = staff1,
              caseNoteText = "Note 2",
            )
          }
        }
        offender(nomsId = "B1234AB") {
          bookingNoCaseNotesId = booking().bookingId
        }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/bookings/${booking.bookingId}/casenotes")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/bookings/${booking.bookingId}/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/bookings/${booking.bookingId}/casenotes")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when booking not found`() {
        webTestClient.get().uri("/bookings/9999/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 200 when booking found with no caseNotes`() {
        webTestClient.get().uri("/bookings/$bookingNoCaseNotesId/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("caseNotes.size()").isEqualTo(0)
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns all caseNotes for current booking`() {
        webTestClient.get().uri("/bookings/${booking.bookingId}/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("caseNotes.size()").isEqualTo(2)
          .jsonPath("caseNotes[0].caseNoteText").isEqualTo("Note 1")
          .jsonPath("caseNotes[1].caseNoteText").isEqualTo("Note 2")
      }
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/casenotes")
  @Nested
  inner class GetCaseNotesForPrisoner {
    private var latestBookingIdA1234AB = 0L
    private var firstBookingIdA1234AB = 0L
    private lateinit var prisoner: Offender
    private lateinit var prisonerNoAlerts: Offender
    private lateinit var prisonerNoBookings: Offender
    private var id1 = 0L
    private var id2 = 0L
    private var id3 = 0L
    private var id4 = 0L
    private var id5 = 0L

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff1 = staff(firstName = "JANE", lastName = "NARK") {
          account(username = "JANE.NARK")
        }
        staff(firstName = "TREVOR", lastName = "NACK") {
          account(username = "TREV.NACK")
        }

        prisoner = offender(nomsId = "A1234AB") {
          latestBookingIdA1234AB = booking(bookingBeginDate = LocalDateTime.parse("2020-01-31T10:00")) {
            id1 = caseNote(
              caseNoteType = "ACP",
              caseNoteSubType = "POPEM",
              author = staff1,
              caseNoteText = "Note 1",
            ).id
            id2 = caseNote(
              caseNoteType = "ACP",
              caseNoteSubType = "POPEM",
              author = staff1,
              caseNoteText = "Note 2",
            ).id
            id3 = caseNote(
              caseNoteType = "ACP",
              caseNoteSubType = "POPEM",
              author = staff1,
              caseNoteText = "Note 3",
            ).id
          }.bookingId
          firstBookingIdA1234AB = booking(bookingBeginDate = LocalDateTime.parse("2016-12-31T10:00")) {
            release(date = LocalDateTime.parse("2017-12-31T10:00"))
            id4 = caseNote(
              caseNoteType = "ACP",
              caseNoteSubType = "POPEM",
              author = staff1,
              caseNoteText = "Note 4",
            ).id
            id5 = caseNote(
              caseNoteType = "ACP",
              caseNoteSubType = "POPEM",
              author = staff1,
              caseNoteText = "Note 5",
            ).id
          }.bookingId
        }
        prisonerNoAlerts = offender(nomsId = "B1234AB") {
          booking()
        }
        prisonerNoBookings = offender(nomsId = "D1234AB")
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteCaseNotes()
      repository.delete(prisoner)
      repository.delete(prisonerNoAlerts)
      repository.delete(prisonerNoBookings)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234AB/casenotes")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when prisoner not found`() {
        webTestClient.get().uri("/prisoners/A9999ZZ/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 200 when prisoner with no bookings found`() {
        webTestClient.get().uri("/prisoners/${prisonerNoBookings.nomsId}/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("caseNotes.size()").isEqualTo(0)
      }

      @Test
      fun `return 200 when prisoner found with no alerts`() {
        webTestClient.get().uri("/prisoners/${prisonerNoAlerts.nomsId}/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("caseNotes.size()").isEqualTo(0)
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns all alerts for current booking`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("caseNotes.size()").isEqualTo(5)
          .jsonPath("caseNotes[0].caseNoteId").isEqualTo(id1)
          .jsonPath("caseNotes[0].bookingId").isEqualTo(latestBookingIdA1234AB)
          .jsonPath("caseNotes[0].caseNoteType.code").isEqualTo("ACP")
          .jsonPath("caseNotes[0].caseNoteSubType.code").isEqualTo("POPEM")
          .jsonPath("caseNotes[0].authorUsername").isEqualTo("JANE.NARK")
          .jsonPath("caseNotes[0].caseNoteText").isEqualTo("Note 1")
          .jsonPath("caseNotes[1].caseNoteId").isEqualTo(id2)
          .jsonPath("caseNotes[1].bookingId").isEqualTo(latestBookingIdA1234AB)
          .jsonPath("caseNotes[1].caseNoteText").isEqualTo("Note 2")
          .jsonPath("caseNotes[2].caseNoteId").isEqualTo(id3)
          .jsonPath("caseNotes[2].bookingId").isEqualTo(latestBookingIdA1234AB)
          .jsonPath("caseNotes[2].caseNoteText").isEqualTo("Note 3")
          .jsonPath("caseNotes[3].caseNoteId").isEqualTo(id4)
          .jsonPath("caseNotes[3].bookingId").isEqualTo(firstBookingIdA1234AB)
          .jsonPath("caseNotes[3].caseNoteText").isEqualTo("Note 4")
          .jsonPath("caseNotes[4].caseNoteId").isEqualTo(id5)
          .jsonPath("caseNotes[4].bookingId").isEqualTo(firstBookingIdA1234AB)
          .jsonPath("caseNotes[4].caseNoteText").isEqualTo("Note 5")
      }
    }
  }

  @DisplayName("GET /bookings/ids")
  @Disabled
  @Nested
  inner class GetBookingIds {
    var bookingId1: Long = 0
    var bookingId2: Long = 0

    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        offender(nomsId = "A1234AB") {
          bookingId1 = booking {}.bookingId
          booking {}
          booking {}
          booking {}
        }
        offender(nomsId = "B1234AB") {
          booking {}
          bookingId2 = booking {}.bookingId
        }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/bookings/ids")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/bookings/ids")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/bookings/ids")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `will return bookingIds`() {
        webTestClient.get().uri {
          it.path("/bookings/ids")
            .queryParam("fromId", "${bookingId1 + 1}")
            .queryParam("toId", "${bookingId2 - 1}")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.numberOfElements").isEqualTo(2)
          .jsonPath("$.content[0]").isEqualTo(bookingId1 + 2)
      }

      @Test
      fun `can filter by just fromId`() {
        webTestClient.get().uri {
          it.path("/bookings/ids")
            .queryParam("fromId", "${bookingId1 + 2}")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.numberOfElements").isEqualTo(3)
      }

      @Test
      fun `can filter by just toId`() {
        webTestClient.get().uri {
          it.path("/bookings/ids")
            .queryParam("toId", "$bookingId2")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.numberOfElements").isEqualTo(5)
      }

      @Test
      fun `will get all ids when there is no filter`() {
        webTestClient.get().uri {
          it.path("/bookings/ids")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.numberOfElements").isEqualTo(6)
      }

      @Test
      fun `will order by booking id ascending`() {
        webTestClient.get().uri {
          it.path("/bookings/ids")
            .build()
        }
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
          .validExchangeBody()
          .jsonPath("$.numberOfElements").isEqualTo(6)
          .jsonPath("$.content[0]").isEqualTo(bookingId1)
          .jsonPath("$.content[5]").isEqualTo(bookingId2)
      }
    }
  }

  private fun <T : RequestHeadersSpec<T>> RequestHeadersSpec<T>.validExchangeBody(): WebTestClient.BodyContentSpec =
    this.headers(setAuthorisation(roles = listOf("ROLE_NOMIS_CASENOTES")))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
}
