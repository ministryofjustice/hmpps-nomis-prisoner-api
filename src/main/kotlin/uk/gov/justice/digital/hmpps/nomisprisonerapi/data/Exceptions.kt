package uk.gov.justice.digital.hmpps.nomisprisonerapi.data

import java.util.function.Supplier

class BadDataException(message: String?) : RuntimeException(message), Supplier<BadDataException> {
  override fun get(): BadDataException {
    return BadDataException(message)
  }
}

class ConflictException(message: String?) : RuntimeException(message), Supplier<ConflictException> {
  override fun get(): ConflictException {
    return ConflictException(message)
  }
}

class NotFoundException(message: String?) : RuntimeException(message), Supplier<NotFoundException> {
  override fun get(): NotFoundException {
    return NotFoundException(message)
  }
}
