package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CSIPFactor

@Repository
interface CSIPFactorRepository :
  CrudRepository<CSIPFactor, Long>,
  JpaSpecificationExecutor<CSIPFactor>
