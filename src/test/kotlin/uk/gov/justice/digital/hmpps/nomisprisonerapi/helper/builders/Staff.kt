package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository

@DslMarker
annotation class StaffDslMarker

@NomisDataDslMarker
interface StaffDsl {
  companion object {
    const val GENERAL = "GENERAL"
    const val ADMIN = "ADMIN"
  }

  @StaffUserAccountDslMarker
  fun account(
    username: String = "G_BYD",
    type: String = GENERAL,
    dsl: StaffUserAccountDsl.() -> Unit = {},
  ): StaffUserAccount
}

@Component
class StaffBuilderFactory(
  private val repository: StaffBuilderRepository,
  private val staffUserAccountBuilderFactory: StaffUserAccountBuilderFactory,
) {
  fun builder(): StaffBuilder = StaffBuilder(repository, staffUserAccountBuilderFactory)
}

@Component
class StaffBuilderRepository(
  private val staffRepository: StaffRepository,
) {
  fun save(staff: Staff): Staff = staffRepository.save(staff)
}

class StaffBuilder(
  private val repository: StaffBuilderRepository,
  private val staffUserAccountBuilderFactory: StaffUserAccountBuilderFactory,
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

  override fun account(username: String, type: String, dsl: StaffUserAccountDsl.() -> Unit): StaffUserAccount =
    staffUserAccountBuilderFactory.builder().let { builder ->
      builder.build(
        username = username,
        staff = staff,
        type = type,
      )
        .also { staff.accounts += it }
        .also { builder.apply(dsl) }
    }
}
