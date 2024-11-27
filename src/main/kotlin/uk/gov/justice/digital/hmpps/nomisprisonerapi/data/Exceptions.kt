package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import java.util.function.Supplier

class BadDataException(message: String?, val error: BadRequestError? = null) :
  RuntimeException(message),
  Supplier<BadDataException> {
  override fun get(): BadDataException = BadDataException(message, error)
}

enum class BadRequestError(val errorCode: Int) {
  ATTENDANCE_PAID(1001),
  PRISONER_MOVED_ALLOCATION_ENDED(1002),
}

class ConflictException(message: String?, val entityId: String? = null) :
  RuntimeException(message),
  Supplier<ConflictException> {
  override fun get(): ConflictException = ConflictException(message, entityId)
}

class NotFoundException(message: String?) :
  RuntimeException(message),
  Supplier<NotFoundException> {
  override fun get(): NotFoundException = NotFoundException(message)
}

class ImageNotFoundException(message: String) : RuntimeException(message)
class ImageBadDataException(message: String) : RuntimeException(message)
