package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.CourseActivityAreas

@Repository
interface CourseAreaRepository : CrudRepository<CourseActivityAreas, Long>
