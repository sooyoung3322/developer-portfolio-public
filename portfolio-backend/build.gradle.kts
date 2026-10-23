plugins {
    id("java")
    id("org.springframework.boot") version "4.1.1"
}

group = "com.portfolio.backend"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    runtimeOnly("org.flywaydb:flyway-mysql")
    runtimeOnly("com.mysql:mysql-connector-j")
    testRuntimeOnly("com.h2database:h2")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Spring Boot 4 defaults to JUnit 6; this project explicitly uses JUnit 5.
    testImplementation(enforcedPlatform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Tests must see the same committed configuration on developer machines and CI.
// Keep processResources/bootRun unchanged; only replace main resources on test
// runtime classpaths, including the dedicated MySQL integration tasks below.
val isolatedTestMainResources = tasks.register<Sync>("isolatedTestMainResources") {
    dependsOn(tasks.processResources)
    from(sourceSets.main.get().output.resourcesDir)
    exclude("application-local.yml", "application-local.yaml", "application-local.properties")
    into(layout.buildDirectory.dir("resources/isolated-test-main"))
}


tasks.bootJar {
    exclude("**/application-local.yml", "**/application-local.yaml", "**/application-local.properties")
}
tasks.withType<Test>().configureEach {
    dependsOn(isolatedTestMainResources)
    classpath = classpath.filter { it != sourceSets.main.get().output.resourcesDir } +
        files(isolatedTestMainResources.map { it.destinationDir })
    doFirst {
        check(sourceSets.main.get().output.resourcesDir !in classpath.files) {
            "Tests must use isolated resources, never developer application-local settings"
        }
    }
}
