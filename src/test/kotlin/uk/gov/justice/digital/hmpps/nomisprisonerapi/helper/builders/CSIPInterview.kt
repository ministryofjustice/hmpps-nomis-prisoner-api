package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInterview
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPInterviewRole
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPReport
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.ReferenceCodeRepository
import java.time.LocalDate

@DslMarker
annotation class CSIPInterviewDslMarker

@NomisDataDslMarker
interface CSIPInterviewDsl

@Component
class CSIPInterviewBuilderFactory(
  private val repository: CSIPInterviewBuilderRepository,
) {
  fun builder() = CSIPInterviewBuilder(repository)
}

@Component
class CSIPInterviewBuilderRepository(
  val interviewRoleRepository: ReferenceCodeRepository<CSIPInterviewRole>,
) {
  fun lookupRole(code: String) = interviewRoleRepository.findByIdOrNull(CSIPInterviewRole.pk(code))!!
}

class CSIPInterviewBuilder(
  private val repository: CSIPInterviewBuilderRepository,
) : CSIPInterviewDsl {
  fun build(
    csipReport: CSIPReport,
    interviewee: String,
    interviewDate: LocalDate,
    role: String,
    comments: String?,
  ): CSIPInterview = CSIPInterview(
    csipReport = csipReport,
    interviewee = interviewee,
    interviewDate = interviewDate,
    role = repository.lookupRole(role),
    comments = comments,
  )
}
