package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.nomisprisonerapi.config.AuditorAwareImpl
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderBookingDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.OffenderDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.NoteSourceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseNote
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TaskSubType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.TaskType
import uk.gov.justice.hmpps.kotlin.auth.HmppsAuthenticationHolder
import uk.gov.justice.hmpps.test.kotlin.auth.WithMockAuthUser
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(HmppsAuthenticationHolder::class, AuditorAwareImpl::class, Repository::class)
@WithMockAuthUser
class CaseNoteRepositoryTest {
  @Autowired
  lateinit var builderRepository: Repository

  @Autowired
  lateinit var repository: OffenderCaseNoteRepository

  @Autowired
  lateinit var taskTypeRepository: ReferenceCodeRepository<TaskType>

  @Autowired
  lateinit var taskSubTypeRepository: ReferenceCodeRepository<TaskSubType>

  lateinit var seedOffenderBooking: OffenderBooking
  lateinit var casenote: OffenderCaseNote

  @Test
  fun getOffenderCaseNote() {
    seedOffenderBooking = builderRepository.save(
      OffenderDataBuilder().withBooking(OffenderBookingDataBuilder()),
    ).latestBooking()

    val timestamp = LocalDateTime.now()

    casenote = repository.save(
      OffenderCaseNote(
        offenderBooking = seedOffenderBooking,
        occurrenceDate = timestamp.toLocalDate(),
        occurrenceDateTime = timestamp,
        caseNoteType = taskTypeRepository.findById(TaskType.pk("ACP")).orElseThrow(),
        caseNoteSubType = taskSubTypeRepository.findById(TaskSubType.pk("POPEM")).orElseThrow(),
        author = builderRepository.save(Staff(firstName = "Joe", lastName = "Bloggs")),
        agencyLocation = seedOffenderBooking.location,
        caseNoteText = "A note",
        amendmentFlag = true,
        noteSourceCode = NoteSourceCode.INST,
        dateCreation = timestamp.toLocalDate(),
        timeCreation = timestamp,
        createdDatetime = timestamp,
        createdUserId = "my-user-name",
      ),
    )

    val offenderCaseNote = repository.findById(casenote.id).orElseThrow()

    with(offenderCaseNote) {
      assertThat(caseNoteType.code).isEqualTo("ACP")
      assertThat(caseNoteSubType.code).isEqualTo("POPEM")
      assertThat(agencyLocation?.id).isEqualTo("BXI")
      assertThat(author.lastName).isEqualTo("Bloggs")
      assertThat(caseNoteText).isEqualTo("A note")
      assertThat(noteSourceCode).isEqualTo(NoteSourceCode.INST)
      assertThat(timeCreation).isEqualTo(timestamp)
      assertThat(createdDatetime).isEqualTo(timestamp)
      assertThat(createdUserId).isEqualTo("my-user-name")
    }
  }
}
