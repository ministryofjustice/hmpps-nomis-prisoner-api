package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IWPTemplate

@Repository
interface IWPTemplateRepository : CrudRepository<IWPTemplate, Long>
