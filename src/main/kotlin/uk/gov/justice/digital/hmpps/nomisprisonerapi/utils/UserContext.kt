package uk.gov.justice.digital.hmpps.nomisprisonerapi.utils

import org.springframework.stereotype.Component

@Component
object UserContext {
  private val authToken = ThreadLocal<String>()
  fun getAuthToken(): String? = authToken.get()
  fun setAuthToken(aToken: String?) = authToken.set(aToken)
}
