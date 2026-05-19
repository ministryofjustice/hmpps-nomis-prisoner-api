package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffInternetAddress
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.UserCaseload
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository
import java.time.LocalDateTime

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
    activeCaseloadId: String? = null,
    caseloads: List<UserCaseload> = listOf(),
    lastLoggedIn: LocalDateTime? = null,
    dsl: StaffUserAccountDsl.() -> Unit = {},
  ): StaffUserAccount

  @StaffEmailDslMarker
  fun email(
    emailAddress: String,
    dsl: StaffEmailDsl.() -> Unit = {},
  ): StaffInternetAddress
}

@Component
class StaffBuilderFactory(
  private val repository: StaffBuilderRepository,
  private val staffEmailBuilderFactory: StaffEmailBuilderFactory,
  private val staffUserAccountBuilderFactory: StaffUserAccountBuilderFactory,
) {
  fun builder(): StaffBuilder = StaffBuilder(repository, staffEmailBuilderFactory, staffUserAccountBuilderFactory)
}

@Component
class StaffBuilderRepository(
  private val staffRepository: StaffRepository,
) {
  fun save(staff: Staff): Staff = staffRepository.save(staff)
}

class StaffBuilder(
  private val repository: StaffBuilderRepository,
  private val staffEmailBuilderFactory: StaffEmailBuilderFactory,
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

  override fun email(
    emailAddress: String,
    dsl: StaffEmailDsl.() -> Unit,
  ): StaffInternetAddress = staffEmailBuilderFactory.builder().let { builder ->
    builder.build(
      staff = staff,
      emailAddress = emailAddress,
    )
      .also { staff.emails += it }
      .also { builder.apply(dsl) }
  }

  override fun account(
    username: String,
    type: String,
    activeCaseloadId: String?,
    caseloads: List<UserCaseload>,
    lastLoggedIn: LocalDateTime?,
    dsl: StaffUserAccountDsl.() -> Unit,
  ): StaffUserAccount = staffUserAccountBuilderFactory.builder().let { builder ->
    builder.build(
      username = username,
      staff = staff,
      type = type,
      activeCaseloadId = activeCaseloadId,
      lastLoggedIn = lastLoggedIn,
    )
      .also { staff.accounts += it }
      .also { builder.apply(dsl) }
  }
}
