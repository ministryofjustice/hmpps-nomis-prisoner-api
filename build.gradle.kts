plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "10.0.0"
  kotlin("plugin.spring") version "2.3.0"
  kotlin("plugin.jpa") version "2.3.0"
  idea
}

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:2.0.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-flyway")
  implementation("org.springframework.boot:spring-boot-jackson2")
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.1")

  implementation("org.flywaydb:flyway-core")
  implementation("org.hibernate.orm:hibernate-community-dialects:7.2.0.Final")
  implementation("com.google.guava:guava:33.5.0-jre")

  runtimeOnly("com.zaxxer:HikariCP")
  implementation("com.h2database:h2:2.4.240")
  runtimeOnly("com.oracle.database.jdbc:ojdbc11:23.26.0.0.0")

  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:2.0.0")
  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
  testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")

  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.mockito:mockito-inline:5.2.0")
  testImplementation("io.swagger.parser.v3:swagger-parser:2.1.37") {
    exclude(group = "io.swagger.core.v3")
  }
  testImplementation("io.swagger.core.v3:swagger-core-jakarta:2.2.41")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

kotlin {
  jvmToolchain(25)
  compilerOptions {
    freeCompilerArgs.addAll("-Xwhen-guards", "-Xannotation-default-target=param-property")
  }
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
  }
}

allOpen {
  annotation("uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen")
}

data class ModelConfiguration(val name: String) {
  fun toTestTaskName(): String = "test${nameToCamel()}"
  private val snakeRegex = "-[a-zA-Z]".toRegex()
  private fun nameToCamel(): String = snakeRegex.replace(name) {
    it.value.replace("-", "").uppercase()
  }.replaceFirstChar { it.uppercase() }
}

val testPackages = listOf(
  ModelConfiguration("activities"),
  ModelConfiguration("adjudications"),
  ModelConfiguration("alerts"),
  ModelConfiguration("contactperson"),
  ModelConfiguration("courtsentencing"),
  ModelConfiguration("movements"),
  ModelConfiguration("sentencingadjustments"),
  ModelConfiguration("visitbalances"),
  ModelConfiguration("visits"),
)

testPackages.forEach {
  val test by testing.suites.existing(JvmTestSuite::class)
  val task = tasks.register<Test>(it.toTestTaskName()) {
    testClassesDirs = files(test.map { it.sources.output.classesDirs })
    classpath = files(test.map { it.sources.runtimeClasspath })
    group = "Run tests"
    description = "Run tests for ${it.name}"
    shouldRunAfter("test")
    useJUnitPlatform()
    filter {
      includeTestsMatching("uk.gov.justice.digital.hmpps.nomisprisonerapi.${it.name}.*")
    }
  }
  tasks.check { dependsOn(task) }
}

tasks.test {
  filter {
    testPackages.forEach {
      excludeTestsMatching("uk.gov.justice.digital.hmpps.nomisprisonerapi.${it.name}.*")
    }
  }
  minHeapSize = "128m"
  maxHeapSize = "2048m"
}
