package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ReferenceCode.Pk

@Repository
interface ReferenceCodeRepository<T : ReferenceCode> : JpaRepository<T, Pk> {
  fun findAllByDomainOrderBySequenceAsc(domain: String): List<T>

  // domain must be included - without it, derived queries are not scoped to the subtype T (the entity hierarchy uses
  // single table inheritance keyed on domain) and can match rows belonging to other reference code domains
  fun findByDomainAndDescription(domain: String, description: String): T?
}
