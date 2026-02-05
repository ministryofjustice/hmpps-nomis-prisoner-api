package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentItem
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderAssessmentItemId

@Repository
interface OffenderAssessmentItemRepository : JpaRepository<OffenderAssessmentItem, OffenderAssessmentItemId>
