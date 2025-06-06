package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Questionnaire

@Repository
interface QuestionnaireRepository :
  CrudRepository<Questionnaire, Long>,
  JpaSpecificationExecutor<Questionnaire> {

  fun findFirstByCodeAndCategory(code: String, category: String): Questionnaire?
  fun findOneByCode(code: String) = findFirstByCodeAndCategory(code, "IR_TYPE")
}
