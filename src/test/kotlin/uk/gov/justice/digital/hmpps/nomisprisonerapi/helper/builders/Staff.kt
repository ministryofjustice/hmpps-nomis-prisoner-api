package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository

@DslMarker
annotation class StaffDslMarker

@NomisDataDslMarker
interface StaffDsl

@Component
class StaffBuilderFactory(
  private val repository: StaffBuilderRepository,
) {
  fun builder(): StaffBuilder {
    return StaffBuilder(repository)
  }
}

@Component
class StaffBuilderRepository(
  private val staffRepository: StaffRepository,
) {
  fun save(staff: Staff): Staff = staffRepository.save(staff)
}

class StaffBuilder(
  private val repository: StaffBuilderRepository,
) : StaffDsl {
  private lateinit var staff: Staff

  fun build(
    lastName: String,
    firstName: String,
  ): Staff = Staff(
    lastName = lastName,
    firstName = firstName,
  )
    .let { repository.save(it) }
    .also { staff = it }
}
