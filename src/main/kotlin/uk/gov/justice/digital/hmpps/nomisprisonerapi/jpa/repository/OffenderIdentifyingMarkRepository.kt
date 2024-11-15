package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifyingMark
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderIdentifyingMarkId

@Repository
interface OffenderIdentifyingMarkRepository : JpaRepository<OffenderIdentifyingMark, OffenderIdentifyingMarkId>
