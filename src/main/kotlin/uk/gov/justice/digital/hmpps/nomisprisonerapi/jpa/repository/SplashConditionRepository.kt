package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.SplashCondition

interface SplashConditionRepository : CrudRepository<SplashCondition, Long>
