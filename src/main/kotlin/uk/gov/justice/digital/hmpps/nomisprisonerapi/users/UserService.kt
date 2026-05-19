package uk.gov.justice.digital.hmpps.nomisprisonerapi.users

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.nomisprisonerapi.data.NotFoundException
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helpers.toAudit
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.StaffUserAccount
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.repository.StaffRepository

@Service
@Transactional
class UserService(
  private val staffRepository: StaffRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getUserDetails(userId: Long): UserDetails = staffRepository.findByIdOrNull(userId)
    ?.toUserDetails()
    ?: throw NotFoundException("User with id=$userId does not exist")
}

fun Staff.toUserDetails(): UserDetails = UserDetails(
  id = id,
  firstName = firstName,
  lastName = lastName,
  // TODO can this ever be empty? check preprod
  email = emails.firstOrNull()?.internetAddress ?: "",
  // TODO status - from DBA_USERS.ACCOUNT_STATUS  e.g ACTIVE, INACTIVE
  statusCode = "ACTIVE",
  accounts = accounts.map { it.toUserAccount() },
  audit = toAudit(),
)

fun StaffUserAccount.toUserAccount() = UserAccount(
  username = username,
  typeCode = type,
  // TODO linked to DBA_USERS.
  //  e.g OPEN, EXPIRED, EXPIRED_GRACE, LOCKED_TIMED, LOCKED, EXPIRED_LOCKED_TIMED, EXPIRED_GRACE_LOCKED_TIMED, EXPIRED_LOCKED, EXPIRED_GRACE_LOCKED,
  statusCode = "OPEN",
  sourceCode = source,
  activeCaseloadId = activeCaseloadId,
  lastLoggedIn = lastLoggedIn,
  caseloads = caseloads.map { it.id.caseloadId },
  audit = toAudit(),
)
