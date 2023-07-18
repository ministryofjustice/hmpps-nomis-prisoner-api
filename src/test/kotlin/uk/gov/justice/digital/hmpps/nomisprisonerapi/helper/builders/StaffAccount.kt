package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffUserAccountRepository

@DslMarker
annotation class StaffUserAccountDslMarker

@NomisDataDslMarker
interface StaffUserAccountDsl

@Component
class StaffUserAccountBuilderFactory(
  private val repository: StaffUserAccountBuilderRepository,
) {
  fun builder(): StaffUserAccountBuilder {
    return StaffUserAccountBuilder(repository)
  }
}

@Component
class StaffUserAccountBuilderRepository(
  private val staffUserAccountRepository: StaffUserAccountRepository,
) {
  fun save(staffUserAccount: StaffUserAccount): StaffUserAccount = staffUserAccountRepository.save(staffUserAccount)
}

class StaffUserAccountBuilder(
  private val repository: StaffUserAccountBuilderRepository,
) : StaffUserAccountDsl {
  private lateinit var staffUserAccount: StaffUserAccount

  fun build(
    username: String,
    staff: Staff,
    type: String,
  ): StaffUserAccount = StaffUserAccount(
    username = username,
    staff = staff,
    type = type,
    source = "USER",
  )
    .let { repository.save(it) }
    .also { staffUserAccount = it }
}
