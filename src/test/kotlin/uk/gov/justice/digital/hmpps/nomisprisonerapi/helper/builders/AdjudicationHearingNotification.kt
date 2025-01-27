package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearing
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingNotification
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AdjudicationHearingNotificationId
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff
import java.time.LocalDate
import java.time.LocalDateTime

@DslMarker
annotation class AdjudicationHearingNotificationDslMarker

@NomisDataDslMarker
interface AdjudicationHearingNotificationDsl

@Component
class AdjudicationHearingNotificationBuilderFactory {
  fun builder(): AdjudicationHearingNotificationBuilder = AdjudicationHearingNotificationBuilder()
}

class AdjudicationHearingNotificationBuilder : AdjudicationHearingNotificationDsl {
  private lateinit var adjudicationHearingNotification: AdjudicationHearingNotification

  fun build(
    hearing: AdjudicationHearing,
    staff: Staff,
    deliveryDateTime: LocalDateTime,
    deliveryDate: LocalDate,
    comment: String?,
    index: Int,
  ): AdjudicationHearingNotification = AdjudicationHearingNotification(
    id = AdjudicationHearingNotificationId(hearing.id, index),
    deliveryStaff = staff,
    deliveryDate = deliveryDate,
    deliveryDateTime = deliveryDateTime,
    comment = comment,
    hearing = hearing,
  )
    .also { adjudicationHearingNotification = it }
}
