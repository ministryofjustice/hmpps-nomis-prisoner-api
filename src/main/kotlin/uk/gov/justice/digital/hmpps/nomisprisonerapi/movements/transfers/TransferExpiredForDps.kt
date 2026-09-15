package uk.gov.justice.digital.hmpps.nomisprisonerapi.movements.transfers

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.EventStatus
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderTransferScheduleOut

internal fun OffenderTransferScheduleOut.transferExpiredForDps(): Boolean = offenderBooking.bookingSequence != 1 && (eventStatus.isScheduled || eventStatus.code == EventStatus.PENDING)
