package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Work

@Repository
interface WorkRepository : JpaRepository<Work, Long> {
  fun findByWorkflowTypeAndWorkTypeAndWorkSubType(
    workflowType: String,
    workType: String,
    workSubtype: String,
  ): Work?
}
