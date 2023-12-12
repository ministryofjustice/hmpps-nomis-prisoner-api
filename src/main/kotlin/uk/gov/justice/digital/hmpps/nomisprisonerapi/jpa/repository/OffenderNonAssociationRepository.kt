package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderNonAssociationId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations.NonAssociationIdResponse

@Repository
interface OffenderNonAssociationRepository :
  CrudRepository<OffenderNonAssociation, OffenderNonAssociationId>, JpaSpecificationExecutor<OffenderNonAssociation> {

  // Filter out duplicates by only returning the "offenderId 1 < offenderId 2" ordered record for each pair of offenders
  @Query(
    """select new uk.gov.justice.digital.hmpps.nomisprisonerapi.nonassociations.NonAssociationIdResponse(o1.nomsId, o2.nomsId)
         from OffenderNonAssociation na, Offender o1, Offender o2 
         where na.id.offenderId = o1.id
           and na.id.nsOffenderId = o2.id
           and na.id.offenderId < na.id.nsOffenderId""",
  )
  fun findAllNomsIds(pageable: Pageable): Page<NonAssociationIdResponse>
}
