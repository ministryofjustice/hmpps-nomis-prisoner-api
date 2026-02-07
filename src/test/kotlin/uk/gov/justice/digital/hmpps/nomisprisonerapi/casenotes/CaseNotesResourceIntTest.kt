package uk.gov.justice.digital.hmpps.nomisprisonerapi.casenotes

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
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
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NoteSourceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderSentence
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCaseNoteRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderSentenceRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.ChronoUnit.SECONDS

class CaseNotesResourceIntTest : IntegrationTestBase() {
  @Autowired
  private lateinit var nomisDataBuilder: NomisDataBuilder

  @Autowired
  private lateinit var offenderCaseNoteRepository: OffenderCaseNoteRepository

  @Autowired
  private lateinit var offenderSentenceRepository: OffenderSentenceRepository

  @Autowired
  private lateinit var repository: Repository

  private lateinit var prisoner: Offender
  private lateinit var booking: OffenderBooking
  private lateinit var staff1: Staff
  private lateinit var casenote1: OffenderCaseNote
  private lateinit var sentence1: OffenderSentence

  private val now = LocalDateTime.now()

  @AfterEach
  fun tearDown() {
    repository.deleteCaseNotes()
    repository.deleteOffenders()
    // TODO
    // repository.deleteStaff()
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returned data for a caseNote`() {
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
                date = LocalDateTime.parse("2021-02-03T04:05:06"),
              )
            }.bookingId
          }
        }

        webTestClient.get().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("caseNoteId").isEqualTo(casenote1.id)
          .jsonPath("bookingId").isEqualTo(bookingId)
          .jsonPath("caseNoteType.code").isEqualTo("ALL")
          .jsonPath("caseNoteSubType.code").isEqualTo("SA")
          .jsonPath("authorUsername").isEqualTo("JANE.NARK")
          .jsonPath("authorStaffId").isEqualTo(staff1.id)
          .jsonPath("authorFirstName").isEqualTo("JANE")
          .jsonPath("authorLastName").isEqualTo("NARK")
          .jsonPath("prisonId").isEqualTo("BXI")
          .jsonPath("caseNoteText").isEqualTo("A note")
          .jsonPath("amendments").isEmpty()
          .jsonPath("occurrenceDateTime").isEqualTo("2021-02-03T04:05:06")
          .jsonPath("creationDateTime").isEqualTo("2021-02-03T04:05:06")
          .jsonPath("noteSourceCode").isEqualTo("INST")
          .jsonPath("createdDatetime").value<String> {
            assertThat(LocalDateTime.parse(it)).isCloseTo(LocalDateTime.now(), within(10, SECONDS))
          }
          .jsonPath("createdUsername").isEqualTo("SA")
          .jsonPath("auditModuleName").isEqualTo("A_MODULE")
          .jsonPath("sourceSystem").isEqualTo("NOMIS")
      }

      @Test
      fun `returned data with null timeCreation defaults to dateCreation`() {
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
                date = LocalDateTime.parse("2021-02-03T04:05:06"),
                timeCreation = null,
              )
            }.bookingId
          }
        }

        webTestClient.get().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus()
          .isOk
          .expectBody()
          .jsonPath("caseNoteId").isEqualTo(casenote1.id)
          .jsonPath("creationDateTime").isEqualTo("2021-02-03T00:00:00")
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
        creationDateTime = LocalDateTime.parse("2021-02-03T04:05:06"),
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
        caseNoteType = "GEN",
        caseNoteSubType = "HIS",
        occurrenceDateTime = LocalDateTime.now(),
        creationDateTime = LocalDateTime.parse("2021-02-03T04:05:06"),
        authorUsername = "JANE.NARK",
        caseNoteText = "the contents",
      )

      @Test
      fun `validation fails when prisoner does not exist`() {
        webTestClient.post().uri("/prisoners/A9999ZZ/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `validation fails when type is not valid`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteType = "NNNNN"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when subtype is not valid`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteSubType = "NNNNN"))
          .exchange()
          .expectStatus().isBadRequest
      }

      @Test
      fun `validation fails when type-subtype combo is not in WORKS`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(caseNoteSubType = "SOU"))
          .exchange()
          .expectStatus().isBadRequest
          .expectBody().jsonPath("$.userMessage").value<String> {
            assertThat(it).isEqualTo("Bad request: CNOTE (type,subtype)=(GEN,SOU) does not exist in the Works table")
          }
      }

      @Test
      fun `validation fails when author does not exist`() {
        webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote.copy(authorUsername = "another"))
          .exchange()
          .expectStatus().isBadRequest
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `creating a case note will allow the data to be retrieved`() {
        val response = webTestClient.post().uri("/prisoners/A1234AB/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(
            //language=JSON
            """
              {
                "caseNoteType": "ACP",
                 "caseNoteSubType": "SOU",
                 "occurrenceDateTime": "2024-06-01T15:00",
                 "creationDateTime": "2024-06-02T15:00",
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
        assertThat(response.bookingId).isEqualTo(booking.bookingId)

        repository.runInTransaction {
          val newCaseNote = offenderCaseNoteRepository.findByIdOrNull(response.id)!!

          assertThat(newCaseNote.id).isEqualTo(response.id)
          assertThat(newCaseNote.offenderBooking.bookingId).isEqualTo(booking.bookingId)
          assertThat(newCaseNote.occurrenceDate).isEqualTo(LocalDate.parse("2024-06-01"))
          assertThat(newCaseNote.occurrenceDateTime).isEqualTo(LocalDateTime.parse("2024-06-01T15:00"))
          assertThat(newCaseNote.caseNoteType.code).isEqualTo("ACP")
          assertThat(newCaseNote.caseNoteSubType.code).isEqualTo("SOU")
          assertThat(newCaseNote.author.lastName).isEqualTo("NARK")
          assertThat(newCaseNote.agencyLocation?.id).isEqualTo("BXI")
          assertThat(newCaseNote.caseNoteText).isEqualTo("the contents")
          assertThat(newCaseNote.amendmentFlag).isFalse()
          assertThat(newCaseNote.noteSourceCode).isEqualTo(NoteSourceCode.INST)
          assertThat(newCaseNote.dateCreation).isEqualTo(LocalDateTime.parse("2024-06-02T00:00"))
          assertThat(newCaseNote.timeCreation).isEqualTo(LocalDateTime.parse("2024-06-02T15:00"))
          assertThat(newCaseNote.createdDatetime).isCloseTo(LocalDateTime.now(), within(5, SECONDS))
          assertThat(newCaseNote.createdUserId).isEqualTo("SA")

          repository.delete(newCaseNote)
        }
      }
    }
  }

  @DisplayName("PUT /casenotes/{caseNoteId}")
  @Nested
  inner class UpdateCaseNote {
    private val validCaseNote = UpdateCaseNoteRequest(
      text = "An amended note",
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `Can update the caseNote and retrieve those caseNote updates`() {
        webTestClient.put().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(validCaseNote)
          .exchange()
          .expectStatus().isEqualTo(200)

        repository.runInTransaction {
          val newCaseNote = offenderCaseNoteRepository.findByIdOrNull(casenote1.id)!!

          assertThat(newCaseNote.occurrenceDate).isEqualTo(now.toLocalDate())
          assertThat(newCaseNote.occurrenceDateTime).isCloseTo(now, within(2, ChronoUnit.MINUTES))
          assertThat(newCaseNote.caseNoteText).isEqualTo("An amended note")
          assertThat(newCaseNote.dateCreation).isEqualTo(newCaseNote.occurrenceDate.atStartOfDay())
          assertThat(newCaseNote.timeCreation).isEqualTo(newCaseNote.occurrenceDateTime)
        }
      }
    }
  }

  @DisplayName("DELETE /casenotes/{caseNoteId}")
  @Nested
  inner class DeleteCaseNote {
    @BeforeEach
    fun setUp() {
      nomisDataBuilder.build {
        staff1 = staff(firstName = "JANE", lastName = "NARK") {
          account(username = "JANE.NARK")
        }
        prisoner = offender(nomsId = "A1234AB") {
          booking {
            casenote1 = caseNote(
              caseNoteType = "ALERT",
              caseNoteSubType = "ACTIVE",
              author = staff1,
              caseNoteText = "A note",
            )
            sentence1 = sentence()
          }
        }
      }
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.delete().uri("/casenotes/1")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.delete().uri("/casenotes/1")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.delete().uri("/casenotes/1")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `validation fails when case note does not exist`() {
        webTestClient.delete().uri("/casenotes/99999")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `Can delete the caseNote`() {
        webTestClient.delete().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isEqualTo(204)

        repository.runInTransaction {
          assertThat(offenderCaseNoteRepository.findByIdOrNull(casenote1.id)).isNull()
        }
      }

      @Test
      fun `Can delete the caseNote when case note sents are present`() {
        repository.addSentenceCaseNoteLink(casenote1.id, sentence1.id.sequence)

        webTestClient.delete().uri("/casenotes/${casenote1.id}")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isEqualTo(204)

        repository.runInTransaction {
          assertThat(offenderCaseNoteRepository.existsById(casenote1.id)).isFalse
          assertThat(offenderSentenceRepository.existsById(sentence1.id)).isTrue
        }
      }
    }
  }

  @DisplayName("GET /prisoners/{offenderNo}/casenotes")
  @Nested
  inner class GetCaseNotesForPrisoner {
    private var latestBookingIdA1234AB = 0L
    private var firstBookingIdA1234AB = 0L
    private lateinit var prisoner: Offender
    private lateinit var prisonerNoCaseNotes: Offender
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
          account(username = "JANE.NARK_ADM", type = "ADMIN")
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
              caseNoteText = "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change",
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
        prisonerNoCaseNotes = offender(nomsId = "B1234AB") {
          booking()
        }
        prisonerNoBookings = offender(nomsId = "D1234AB")
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteCaseNotes()
      repository.delete(prisoner)
      repository.delete(prisonerNoCaseNotes)
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
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 200 when prisoner with no bookings found`() {
        webTestClient.get().uri("/prisoners/${prisonerNoBookings.nomsId}/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("caseNotes.size()").isEqualTo(0)
      }

      @Test
      fun `return 200 when prisoner found with no casenotes`() {
        webTestClient.get().uri("/prisoners/${prisonerNoCaseNotes.nomsId}/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("caseNotes.size()").isEqualTo(0)
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns all casenotes for current booking`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/casenotes")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
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
          .jsonPath("caseNotes[0].authorUsernames.size()").isEqualTo(2)
          .jsonPath("caseNotes[0].authorUsernames[0]").isEqualTo("JANE.NARK")
          .jsonPath("caseNotes[0].authorUsernames[1]").isEqualTo("JANE.NARK_ADM")
          .jsonPath("caseNotes[0].caseNoteText").isEqualTo("Note 1")
          .jsonPath("caseNotes[0].amendments.length()").isEqualTo(0)
          .jsonPath("caseNotes[1].caseNoteId").isEqualTo(id2)
          .jsonPath("caseNotes[1].bookingId").isEqualTo(latestBookingIdA1234AB)
          .jsonPath("caseNotes[1].caseNoteText").isEqualTo("basic text")
          .jsonPath("caseNotes[1].amendments[0].text").isEqualTo("made a change")
          .jsonPath("caseNotes[1].amendments[0].authorUsername").isEqualTo("JMORROW_GEN")
          .jsonPath("caseNotes[1].amendments[0].createdDateTime").isEqualTo("2023-03-02T17:11:41")
          .jsonPath("caseNotes[1].amendments[0].sourceSystem").isEqualTo("NOMIS")
          .jsonPath("caseNotes[1].amendments.length()").isEqualTo(1)
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

  @DisplayName("GET /prisoners/{offenderNo}/casenotes/reconciliation")
  @Nested
  inner class GetCaseNotesForPrisonerForReconciliation {
    private var latestBookingIdA1234AB = 0L
    private var firstBookingIdA1234AB = 0L
    private lateinit var prisoner: Offender
    private lateinit var prisonerNoCaseNotes: Offender
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
          account(username = "JANE.NARK_ADM", type = "ADMIN")
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
              caseNoteText = "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change",
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
        prisonerNoCaseNotes = offender(nomsId = "B1234AB") {
          booking()
        }
        prisonerNoBookings = offender(nomsId = "D1234AB")
      }
    }

    @AfterEach
    fun tearDown() {
      repository.deleteCaseNotes()
      repository.delete(prisoner)
      repository.delete(prisonerNoCaseNotes)
      repository.delete(prisonerNoBookings)
    }

    @Nested
    inner class Security {
      @Test
      fun `access forbidden when no role`() {
        webTestClient.get().uri("/prisoners/A1234AB/casenotes/reconciliation")
          .headers(setAuthorisation(roles = listOf()))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access forbidden with wrong role`() {
        webTestClient.get().uri("/prisoners/A1234AB/casenotes/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_BANANAS")))
          .exchange()
          .expectStatus().isForbidden
      }

      @Test
      fun `access unauthorised with no auth token`() {
        webTestClient.get().uri("/prisoners/A1234AB/casenotes/reconciliation")
          .exchange()
          .expectStatus().isUnauthorized
      }
    }

    @Nested
    inner class Validation {
      @Test
      fun `return 404 when prisoner not found`() {
        webTestClient.get().uri("/prisoners/A9999ZZ/casenotes/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isNotFound
      }

      @Test
      fun `return 200 when prisoner with no bookings found`() {
        webTestClient.get().uri("/prisoners/${prisonerNoBookings.nomsId}/casenotes/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("caseNotes.size()").isEqualTo(0)
      }

      @Test
      fun `return 200 when prisoner found with no casenotes`() {
        webTestClient.get().uri("/prisoners/${prisonerNoCaseNotes.nomsId}/casenotes/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("caseNotes.size()").isEqualTo(0)
      }
    }

    @Nested
    inner class HappyPath {
      @Test
      fun `returns all casenotes for current booking`() {
        webTestClient.get().uri("/prisoners/${prisoner.nomsId}/casenotes/reconciliation")
          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_PRISONER_API__SYNCHRONISATION__RW")))
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
          .jsonPath("caseNotes[0].authorUsernames.size()").isEqualTo(2)
          .jsonPath("caseNotes[0].authorUsernames[0]").isEqualTo("JANE.NARK")
          .jsonPath("caseNotes[0].authorUsernames[1]").isEqualTo("JANE.NARK_ADM")
          .jsonPath("caseNotes[0].caseNoteText").isEqualTo("Note 1")
          .jsonPath("caseNotes[1].caseNoteId").isEqualTo(id2)
          .jsonPath("caseNotes[1].bookingId").isEqualTo(latestBookingIdA1234AB)
          .jsonPath("caseNotes[1].caseNoteText").isEqualTo("basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change")
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
}
