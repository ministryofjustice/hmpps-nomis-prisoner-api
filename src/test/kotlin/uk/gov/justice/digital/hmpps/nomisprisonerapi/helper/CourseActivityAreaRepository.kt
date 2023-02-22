package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityArea

@Repository
interface CourseActivityAreaRepository : CrudRepository<CourseActivityArea, Long>
