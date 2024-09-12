package uk.gov.justice.digital.hmpps.nomisprisonerapi.casenotes

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
import java.time.LocalDateTime

internal class CaseNotesServiceTest {

  private val offenderRepository: OffenderRepository = mock()
  private val offenderBookingRepository: OffenderBookingRepository = mock()
  private val offenderCaseNoteRepository: OffenderCaseNoteRepository = mock()
  private val staffUserAccountRepository: StaffUserAccountRepository = mock()
  private val taskTypeRepository: ReferenceCodeRepository<TaskType> = mock()
  private val taskSubTypeRepository: ReferenceCodeRepository<TaskSubType> = mock()

  private val locationService = CaseNotesService(
    offenderRepository,
    offenderBookingRepository,
    offenderCaseNoteRepository,
    staffUserAccountRepository,
    taskTypeRepository,
    taskSubTypeRepository,
  )

  @Nested
  internal inner class ParseMainText {

    @Test
    fun `basic text is copied correctly`() {
      assertThat(locationService.parseMainText("basic text")).isEqualTo("basic text")
    }

    @Test
    fun `null text`() {
      assertThat(locationService.parseMainText(null)).isNull()
    }

    @Test
    fun `empty text`() {
      assertThat(locationService.parseMainText("")).isEqualTo("")
    }

    @Test
    fun `text truncated before amendment`() {
      assertThat(
        locationService.parseMainText(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change",
        ),
      )
        .isEqualTo("basic text")
    }

    @Test
    fun `text truncated before multiple amendments`() {
      assertThat(
        locationService.parseMainText(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change ...[PPHILLIPS_GEN updated the case notes on 2023/06/28 15:52:08] with more details",
        ),
      )
        .isEqualTo("basic text")
    }
  }

  @Nested
  internal inner class ParseAmendments {

    @Test
    fun `basic text has no amendments`() {
      assertThat(locationService.parseAmendments("basic text")).isEmpty()
    }

    @Test
    fun `null text`() {
      assertThat(locationService.parseAmendments(null)).isEmpty()
    }

    @Test
    fun `empty text`() {
      assertThat(locationService.parseAmendments("")).isEmpty()
    }

    @Test
    fun `one amendment`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(
        StaffUserAccount(
          username = "JMORROW_GEN",
          Staff(12345L, "First1", "Last1"),
          "type",
          "source",
        ),
      )

      assertThat(
        locationService.parseAmendments(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change",
        ),
      )
        .containsExactly(
          CaseNoteAmendment(
            "made a change",
            "JMORROW_GEN",
            12345L,
            "First1 Last1",
            LocalDateTime.parse("2023-03-02T17:11:41"),
          ),
        )
    }

    @Test
    fun `multiple amendments`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(
        StaffUserAccount(
          username = "JMORROW_GEN",
          Staff(12345L, "First1", "Last1"),
          "type",
          "source",
        ),
      )
      whenever(staffUserAccountRepository.findByUsername("PPHILLIPS_GEN")).thenReturn(
        StaffUserAccount(
          username = "PPHILLIPS_GEN",
          Staff(67890L, "First2", "Last2"),
          "type",
          "source",
        ),
      )

      assertThat(
        locationService.parseAmendments(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] made a change ...[PPHILLIPS_GEN updated the case notes on 2023/06/28 15:52:08] with more details",
        ),
      )
        .containsExactly(
          CaseNoteAmendment(
            "made a change",
            "JMORROW_GEN",
            12345L,
            "First1 Last1",
            LocalDateTime.parse("2023-03-02T17:11:41"),
          ),
          CaseNoteAmendment(
            "with more details",
            "PPHILLIPS_GEN",
            67890L,
            "First2 Last2",
            LocalDateTime.parse("2023-06-28T15:52:08"),
          ),
        )
    }

    @Test
    fun `one empty amendment`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(
        StaffUserAccount(
          username = "JMORROW_GEN",
          Staff(12345L, "First1", "Last1"),
          "type",
          "source",
        ),
      )

      assertThat(
        locationService.parseAmendments(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41] ",
        ),
      )
        .containsExactly(
          CaseNoteAmendment(
            "",
            "JMORROW_GEN",
            12345L,
            "First1 Last1",
            LocalDateTime.parse("2023-03-02T17:11:41"),
          ),
        )
    }

    @Test
    fun `multiple amendments includes empty`() {
      whenever(staffUserAccountRepository.findByUsername("JMORROW_GEN")).thenReturn(
        StaffUserAccount(
          username = "JMORROW_GEN",
          Staff(12345L, "First1", "Last1"),
          "type",
          "source",
        ),
      )
      whenever(staffUserAccountRepository.findByUsername("PPHILLIPS_GEN")).thenReturn(
        StaffUserAccount(
          username = "PPHILLIPS_GEN",
          Staff(67890L, "First2", "Last2"),
          "type",
          "source",
        ),
      )

      assertThat(
        locationService.parseAmendments(
          "basic text ...[JMORROW_GEN updated the case notes on 2023/03/02 17:11:41]  ...[PPHILLIPS_GEN updated the case notes on 2023/06/28 15:52:08] with more details",
        ),
      )
        .containsExactly(
          CaseNoteAmendment(
            "",
            "JMORROW_GEN",
            12345L,
            "First1 Last1",
            LocalDateTime.parse("2023-03-02T17:11:41"),
          ),
          CaseNoteAmendment(
            "with more details",
            "PPHILLIPS_GEN",
            67890L,
            "First2 Last2",
            LocalDateTime.parse("2023-06-28T15:52:08"),
          ),
        )
    }
  }
}
