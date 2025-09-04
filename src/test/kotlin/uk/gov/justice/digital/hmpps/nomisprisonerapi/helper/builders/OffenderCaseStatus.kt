package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourtCase
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderCaseStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.OffenderCaseStatusRepository

@DslMarker
annotation class OffenderCaseStatusDslMarker

@NomisDataDslMarker
interface OffenderCaseStatusDsl

@Component
class OffenderCaseStatusBuilderFactory(
  private val repository: OffenderCaseStatusBuilderRepository,
) {
  fun builder(): OffenderCaseStatusBuilder = OffenderCaseStatusBuilder(
    repository,
  )
}

@Component
class OffenderCaseStatusBuilderRepository(
  val repository: OffenderCaseStatusRepository,
) {
  fun save(offenderCaseStatus: OffenderCaseStatus): OffenderCaseStatus = repository.save(offenderCaseStatus)
}

class OffenderCaseStatusBuilder(
  private val repository: OffenderCaseStatusBuilderRepository,
) : OffenderCaseStatusDsl {

  fun build(
    courtCase: CourtCase,
    statusUpdateStaff: Staff,
  ): OffenderCaseStatus = OffenderCaseStatus(
    courtCase = courtCase,
    statusUpdateReason = "1",
    staffId = statusUpdateStaff.id,
  )
    .let { repository.save(it) }
}
