package uk.gov.justice.digital.hmpps.nomisprisonerapi.casenotes

import com.google.common.base.Utf8
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
import java.time.LocalDateTime

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
      assertThat(caseNotesService.parseAmendments("basic text")).isEmpty()
    }

    @Test
    fun `empty text`() {
      assertThat(caseNotesService.parseAmendments("")).isEmpty()
    }

    @Test
    fun `one amendment`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)

      assertThat(
        caseNotesService.parseAmendments(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change",
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
          ),
        )
    }

    @Test
    fun `one old-style amendment`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)

      assertThat(
        caseNotesService.parseAmendments(
          "basic text ...[JMORROW_GEN updated the case note on 14/12/2006 07:32:39] made a change",
        ),
      )
        .extracting("createdDateTime").containsExactly(LocalDateTime.parse("2006-12-14T07:32:39"))
    }

    @Test
    fun `one middle-era-style amendment`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)

      assertThat(
        caseNotesService.parseAmendments(
          "basic text ...[JMORROW_GEN updated the case notes on 18-08-2009 14:04:53] made a change",
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
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change ...[PPHILLIPS_GEN updated the case notes on 2023/06/28 15:52:08] with more details",
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
          ),
          CaseNoteAmendment(
            "with more details",
            "PPHILLIPS_GEN",
            67890L,
            "First2",
            "Last2",
            LocalDateTime.parse("2023-06-28T15:52:08"),
          ),
        )
    }

    @Test
    fun `one empty amendment`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)

      assertThat(
        caseNotesService.parseAmendments(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] ",
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
          ),
        )
    }

    @Test
    fun `multiple amendments includes empty`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(staffUserAccount1)
      whenever(staffUserAccountRepository.findByUsername("PPHILLIPS_GEN")).thenReturn(staffUserAccount2)

      assertThat(
        caseNotesService.parseAmendments(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41]  ...[PPHILLIPS_GEN updated the case notes on 2023/06/28 15:52:08] with more details",
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
          ),
          CaseNoteAmendment(
            "with more details",
            "PPHILLIPS_GEN",
            67890L,
            "First2",
            "Last2",
            LocalDateTime.parse("2023-06-28T15:52:08"),
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
}
