package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IEPLevel
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Incentive
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.IncentiveId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import java.time.LocalDateTime

class IncentiveBuilder(
  var iepLevel: String = "ENT",
  var userId: String? = null,
  var sequence: Long = 1,
  var commentText: String = "comment",
  var auditModuleName: String? = null,
  var iepDateTime: LocalDateTime = LocalDateTime.now(),
) {
  fun build(
    offenderBooking: OffenderBooking,
    level: IEPLevel,
  ): Incentive =
    Incentive(
      id = IncentiveId(offenderBooking = offenderBooking, sequence = sequence),
      commentText = commentText,
      iepDate = iepDateTime.toLocalDate(),
      iepTime = iepDateTime,
      location = offenderBooking.location!!,
      iepLevel = level,
      userId = userId,
      auditModuleName = auditModuleName,
    )
}
