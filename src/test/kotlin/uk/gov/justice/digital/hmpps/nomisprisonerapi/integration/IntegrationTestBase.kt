package uk.gov.justice.digital.hmpps.nomisprisonerapi.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.NomisDataBuilder
import uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders.Repository
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @MockitoSpyBean
  protected lateinit var telemetryClient: TelemetryClient

  @Autowired
  lateinit var repository: Repository

  @Autowired
  lateinit var nomisDataBuilder: NomisDataBuilder

  internal fun setAuthorisation(
    user: String = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = user, roles = roles, scope = scopes)

  fun deleteOffenders() {
    repository.deleteOffenders()
  }
}

inline fun <reified B : Any> WebTestClient.ResponseSpec.expectBodyResponse(): B = this.expectStatus().is2xxSuccessful.expectBody(B::class.java).returnResult().responseBody!!
