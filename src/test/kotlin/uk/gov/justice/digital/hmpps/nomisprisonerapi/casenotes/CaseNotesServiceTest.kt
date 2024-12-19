package uk.gov.justice.digital.hmpps.nomisprisonerapi.casenotes

import com.google.common.base.Utf8
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Gender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NoteSourceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Offender
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TaskSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TaskType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderBookingRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCaseNoteRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.WorkRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

private val TWO_UNICODE_CHARS = "⌘⌥"

internal class CaseNotesServiceTest {

  private val offenderRepository: OffenderRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val offenderCaseNoteRepository: OffenderCaseNoteRepository = mock()
  private val staffUserAccountRepository: StaffUserAccountRepository = mock()
  private val taskTypeRepository: ReferenceCodeRepository<TaskType> = mock()
  private val taskSubTypeRepository: ReferenceCodeRepository<TaskSubType> = mock()
  private val workRepository: WorkRepository = mock()

  private val caseNotesService = CaseNotesService(
    offenderRepository,
    offenderBookingRepository,
    offenderCaseNoteRepository,
    staffUserAccountRepository,
    taskTypeRepository,
    taskSubTypeRepository,
    workRepository,
  )

  @Nested
  internal inner class ParseMainText {

    @Test
    fun `basic text is copied correctly`() {
      assertThat(caseNotesService.parseMainText("basic text")).isEqualTo("basic text")
    }

    @Test
    fun `empty text`() {
      assertThat(caseNotesService.parseMainText("")).isEqualTo("")
    }

    @Test
    fun `text truncated before amendment`() {
      assertThat(
        caseNotesService.parseMainText(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change",
        ),
      )
        .isEqualTo("basic text")
    }

    @Test
    fun `text truncated before multiple amendments`() {
      assertThat(
        caseNotesService.parseMainText(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change ...[PPHILLIPS_GEN updated the case notes on 2023/06/28 15:52:08] with more details",
        ),
      )
        .isEqualTo("basic text")
    }
  }

  @Nested
  internal inner class ParseAmendments {

    private val staffUserAccount1 = StaffUserAccount(
      username = "JMORROW_GEN",
      Staff(12345L, "First1", "Last1"),
      "type",
      "source",
    )
    private val staffUserAccount2 = StaffUserAccount(
      username = "PPHILLIPS_GEN",
      Staff(67890L, "First2", "Last2"),
      "type",
      "source",
    )

    @Test
    fun `basic text has no amendments`() {
      assertThat(caseNotesService.parseAmendments(caseNote("basic text"))).isEmpty()
    }

    @Test
    fun `empty text`() {
      assertThat(caseNotesService.parseAmendments(caseNote(""))).isEmpty()
    }

    @Test
    fun `one amendment`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)

      assertThat(
        caseNotesService.parseAmendments(
          caseNote("basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change"),
        ),
      )
        .containsExactly(
          CaseNoteAmendment(
            "made a change",
            "JMORROW_GEN",
            12345L,
            "First1",
            "Last1",
            LocalDateTime.parse("2023-03-02T17:11:41"),
            sourceSystem = SourceSystem.NOMIS,
          ),
        )
    }

    @Test
    fun `one old-style amendment`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)

      assertThat(
        caseNotesService.parseAmendments(
          caseNote("basic text ...[JMORROW_GEN updated the case note on 14/12/2006 07:32:39] made a change"),
        ),
      )
        .extracting("createdDateTime").containsExactly(LocalDateTime.parse("2006-12-14T07:32:39"))
    }

    @Test
    fun `one middle-era-style amendment`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)

      assertThat(
        caseNotesService.parseAmendments(
          caseNote("basic text ...[JMORROW_GEN updated the case notes on 18-08-2009 14:04:53] made a change"),
        ),
      )
        .extracting("createdDateTime").containsExactly(LocalDateTime.parse("2009-08-18T14:04:53"))
    }

    @Test
    fun `multiple amendments`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)
      whenever(staffUserAccountRepository.findByUsername("PPHILLIPS_GEN")).thenReturn(staffUserAccount2)

      assertThat(
        caseNotesService.parseAmendments(
          caseNote("basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change ...[PPHILLIPS_GEN updated the case notes on 2023/06/28 15:52:08] with more details"),
        ),
      )
        .containsExactly(
          CaseNoteAmendment(
            "made a change",
            "JMORROW_GEN",
            12345L,
            "First1",
            "Last1",
            LocalDateTime.parse("2023-03-02T17:11:41"),
            sourceSystem = SourceSystem.NOMIS,
          ),
          CaseNoteAmendment(
            "with more details",
            "PPHILLIPS_GEN",
            67890L,
            "First2",
            "Last2",
            LocalDateTime.parse("2023-06-28T15:52:08"),
            sourceSystem = SourceSystem.NOMIS,
          ),
        )
    }

    @Test
    fun `multiple amendments with CRLF`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)

      assertThat(
        caseNotesService.parseAmendments(
          caseNote(
            """Didn't attend work, and had no valid reason to not attend.
This is not acceptable behaviour. ...[JMORROW_GEN updated the case notes on 2023/07/11 15:19:11] Please dismiss the above, Mr Beckford was not feeling well, 
Also now has a sick note from health care until 14/07/2023. ...[JMORROW_GEN updated the case notes on 2023/07/11 15:19:11] Please dismiss the above, Mr Beckford was not feeling well, 
Also now has a sick note from health care until 14/07/2023.""",
          ),
        ),
      )
        .containsExactly(
          CaseNoteAmendment(
            """Please dismiss the above, Mr Beckford was not feeling well, 
Also now has a sick note from health care until 14/07/2023.""",
            "JMORROW_GEN",
            12345L,
            "First1",
            "Last1",
            LocalDateTime.parse("2023-07-11T15:19:11"),
            sourceSystem = SourceSystem.NOMIS,
          ),
          CaseNoteAmendment(
            """Please dismiss the above, Mr Beckford was not feeling well, 
Also now has a sick note from health care until 14/07/2023.""",
            "JMORROW_GEN",
            12345L,
            "First1",
            "Last1",
            LocalDateTime.parse("2023-07-11T15:19:11"),
            sourceSystem = SourceSystem.NOMIS,
          ),
        )
    }

    // Didn't attend work, and had no valid reason to not attend.
    // This is not acceptable behaviour. ...[NQL56L updated the case notes on 2023/07/11 15:19:11] Please dismiss the above, Mr Beckford was not feeling well,
    // Also now has a sick note from health care until 14/07/2023. ...[NQL56L updated the case notes on 2023/07/11 15:19:11] Please dismiss the above, Mr Beckford was not feeling well,
    // Also now has a sick note from health care until 14/07/2023.

    @Test
    fun `one empty amendment`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)

      assertThat(
        caseNotesService.parseAmendments(
          caseNote("basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] "),
        ),
      )
        .containsExactly(
          CaseNoteAmendment(
            "",
            "JMORROW_GEN",
            12345L,
            "First1",
            "Last1",
            LocalDateTime.parse("2023-03-02T17:11:41"),
            sourceSystem = SourceSystem.NOMIS,
          ),
        )
    }

    @Test
    fun `multiple amendments includes empty`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)
      whenever(staffUserAccountRepository.findByUsername("PPHILLIPS_GEN")).thenReturn(staffUserAccount2)

      assertThat(
        caseNotesService.parseAmendments(
          caseNote("basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41]  ...[PPHILLIPS_GEN updated the case notes on 2023/06/28 15:52:08] with more details"),
        ),
      )
        .containsExactly(
          CaseNoteAmendment(
            "",
            "JMORROW_GEN",
            12345L,
            "First1",
            "Last1",
            LocalDateTime.parse("2023-03-02T17:11:41"),
            sourceSystem = SourceSystem.NOMIS,
          ),
          CaseNoteAmendment(
            "with more details",
            "PPHILLIPS_GEN",
            67890L,
            "First2",
            "Last2",
            LocalDateTime.parse("2023-06-28T15:52:08"),
            sourceSystem = SourceSystem.NOMIS,
          ),
        )
    }
  }

  @Nested
  internal inner class ReconstructText {
    @Test
    fun `plain unamended text`() {
      assertThat(
        caseNotesService.reconstructText(
          UpdateCaseNoteRequest(
            text = "some text",
            amendments = emptyList(),
          ),
        ),
      ).isEqualTo("some text")
    }

    @Test
    fun `one amendment`() {
      assertThat(
        caseNotesService.reconstructText(
          UpdateCaseNoteRequest(
            text = "some text",
            amendments = listOf(
              UpdateAmendment(
                text = "change",
                authorUsername = "STEVER",
                createdDateTime = LocalDateTime.parse("2024-05-06T07:08:09"),
              ),
            ),
          ),
        ),
      ).isEqualTo("some text ...[STEVER updated the case notes on 2024/05/06 07:08:09] change")
    }

    @Test
    fun `multiple amendments`() {
      assertThat(
        caseNotesService.reconstructText(
          UpdateCaseNoteRequest(
            text = "some text",
            amendments = listOf(
              UpdateAmendment(
                text = "change",
                authorUsername = "STEVER",
                createdDateTime = LocalDateTime.parse("2024-05-06T07:08:09"),
              ),
              UpdateAmendment(
                text = "another",
                authorUsername = "STEREN",
                createdDateTime = LocalDateTime.parse("2024-05-07T07:08:10"),
              ),
            ),
          ),
        ),
      ).isEqualTo("some text ...[STEVER updated the case notes on 2024/05/06 07:08:09] change ...[STEREN updated the case notes on 2024/05/07 07:08:10] another")
    }

    @Test
    fun `truncation without amendments`() {
      assertThat(
        caseNotesService.reconstructText(
          UpdateCaseNoteRequest(
            text = "s".repeat(4000),
            amendments = emptyList(),
          ),
        ),
      ).isEqualTo("s".repeat(4000))

      assertThat(
        caseNotesService.reconstructText(
          UpdateCaseNoteRequest(
            text = "s".repeat(4001),
            amendments = emptyList(),
          ),
        ),
      ).isEqualTo("${"s".repeat(3975)}... see DPS for full text")
    }

    @Test
    fun `truncation with amendments`() {
      assertThat(
        caseNotesService.reconstructText(
          UpdateCaseNoteRequest(
            text = "s".repeat(3960),
            amendments = listOf(
              UpdateAmendment(
                text = "change",
                authorUsername = "STEVER",
                createdDateTime = LocalDateTime.parse("2024-05-06T07:08:09"),
              ),
            ),
          ),
        ),
      ).isEqualTo("${"s".repeat(3960)} ...[STEVER upd... see DPS for full text")
    }

    @Test
    fun `truncation with unicode length ok`() {
      val textOk = "s".repeat(3994) + TWO_UNICODE_CHARS
      assertThat(
        caseNotesService.reconstructText(
          UpdateCaseNoteRequest(
            text = textOk,
            amendments = emptyList(),
          ),
        ),
      ).isEqualTo(textOk)
    }

    @Test
    fun `truncation with unicode too long unicode at end`() {
      val textTooLong = "s".repeat(3995) + TWO_UNICODE_CHARS
      val result = caseNotesService.reconstructText(
        UpdateCaseNoteRequest(
          text = textTooLong,
          amendments = emptyList(),
        ),
      )
      assertThat(result).isEqualTo("${"s".repeat(3971)}... see DPS for full text")
      assertThat(Utf8.encodedLength(result)).isEqualTo(3996) // shorter than 4000 because some unicode has been truncated
    }

    @Test
    fun `truncation with unicode too long unicode at start`() {
      val textTooLongUnicodeAtStart = TWO_UNICODE_CHARS + "s".repeat(3995)
      val result = caseNotesService.reconstructText(
        UpdateCaseNoteRequest(
          text = textTooLongUnicodeAtStart,
          amendments = emptyList(),
        ),
      )
      assertThat(result).isEqualTo("${textTooLongUnicodeAtStart.substring(0..3970)}... see DPS for full text")
      assertThat(Utf8.encodedLength(result)).isEqualTo(4000)
    }
  }

  @Nested
  internal inner class CaseNoteSourceSystem {
    @Test
    fun `Is NOMIS`() {
      whenever(offenderCaseNoteRepository.findById(1)).thenReturn(
        Optional.of(caseNote()),
      )
      assertThat(
        caseNotesService.getCaseNote(1L).sourceSystem,
      ).isEqualTo(
        SourceSystem.NOMIS,
      )
    }

    @Test
    fun `Is DPS due to null modify user null`() {
      whenever(offenderCaseNoteRepository.findById(1)).thenReturn(
        Optional.of(caseNote(auditModuleName = "PRISON_API", modifiedUserId = null)),
      )
      assertThat(
        caseNotesService.getCaseNote(1L).sourceSystem,
      ).isEqualTo(
        SourceSystem.DPS,
      )
    }

    @Test
    fun `Is DPS due to modify user equals create`() {
      whenever(offenderCaseNoteRepository.findById(1)).thenReturn(
        Optional.of(caseNote(auditModuleName = "ELITE2_API", modifiedUserId = "created-user_id")),
      )
      assertThat(
        caseNotesService.getCaseNote(1L).sourceSystem,
      ).isEqualTo(
        SourceSystem.DPS,
      )
    }
  }

  @Nested
  internal inner class AmendmentSourceSystem {
    val textWithAmendment = "some text ...[STEVER updated the case notes on 2024/05/06 07:08:09] change"

    @Test
    fun `Is NOMIS`() {
      whenever(offenderCaseNoteRepository.findById(1)).thenReturn(
        Optional.of(caseNote(textWithAmendment)),
      )
      assertThat(
        caseNotesService.getCaseNote(1L).amendments.first().sourceSystem,
      ).isEqualTo(
        SourceSystem.NOMIS,
      )
    }

    @Test
    fun `Is NOMIS due to null modify date`() {
      whenever(offenderCaseNoteRepository.findById(1)).thenReturn(
        Optional.of(caseNote(textWithAmendment, auditModuleName = "PRISON_API", modifiedDateTime = null)),
      )
      assertThat(
        caseNotesService.getCaseNote(1L).amendments.first().sourceSystem,
      ).isEqualTo(
        SourceSystem.NOMIS,
      )
    }

    @Test
    fun `Is DPS due to modify date not null`() {
      whenever(offenderCaseNoteRepository.findById(1)).thenReturn(
        Optional.of(caseNote(textWithAmendment, auditModuleName = "PRISON_API")),
      )
      assertThat(
        caseNotesService.getCaseNote(1L).amendments.first().sourceSystem,
      ).isEqualTo(
        SourceSystem.DPS,
      )
    }
  }
}

fun caseNote(
  caseNoteText: String = "A note",
  auditModuleName: String = "a-module",
  modifiedUserId: String? = "my-user-name",
  modifiedDateTime: LocalDateTime? = LocalDateTime.now(),
) =
  OffenderCaseNote(
    offenderBooking = OffenderBooking(
      offender = Offender(
        nomsId = "A1234AA",
        gender = Gender("MALE", "DESC"),
        firstName = "First",
        lastName = "Last",
      ),
      bookingBeginDate = LocalDateTime.now(),
    ),
    occurrenceDate = LocalDate.parse("2024-03-04"),
    occurrenceDateTime = LocalDateTime.now(),
    caseNoteType = TaskType("CODE", "desc"),
    caseNoteSubType = TaskSubType("SUBCODE", "desc"),
    author = Staff(firstName = "Joe", lastName = "Bloggs")
      .apply {
        accounts.add(StaffUserAccount(username = "USER", staff = this, type = "type", source = "source"))
      },
    caseNoteText = caseNoteText,
    amendmentFlag = true,
    noteSourceCode = NoteSourceCode.INST,

    dateCreation = LocalDateTime.now(),
    createdUserId = "created-user_id",
    auditModuleName = auditModuleName,

    modifiedUserId = modifiedUserId,
    modifiedDatetime = modifiedDateTime,
  )
