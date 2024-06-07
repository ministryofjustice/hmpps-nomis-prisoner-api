package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributeId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderPhysicalAttributes

@Repository
interface OffenderPhysicalAttributesRepository : JpaRepository<OffenderPhysicalAttributes, OffenderPhysicalAttributeId>
