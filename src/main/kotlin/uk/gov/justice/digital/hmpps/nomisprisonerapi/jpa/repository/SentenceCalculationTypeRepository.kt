package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SentenceCalculationTypeId

@Repository
interface SentenceCalculationTypeRepository :
  CrudRepository<SentenceCalculationType, SentenceCalculationTypeId>,
  JpaSpecificationExecutor<SentenceCalculationType>
