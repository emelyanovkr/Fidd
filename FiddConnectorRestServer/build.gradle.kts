plugins {
    id("java")
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.16.0"
}

group = "com.fidd"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    named("main") {
        java.srcDir("src/main/java")
        java.srcDir(layout.buildDirectory.dir("generated/src/main/java"))
    }
    named("test") {
        java.srcDir("src/test/java")
    }
}

dependencies {
    implementation(project(":FiddCore"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.openapitools:jackson-databind-nullable:0.2.10")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/FiddConnectorRestServer/src/main/resources/openapi/openapi.yaml")
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath)
    apiPackage.set("com.fidd.connector.controller")
    modelPackage.set("com.fidd.connector.model")
    invokerPackage.set("com.fidd.connector.invoker")

    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)

    configOptions.set(
        mapOf(
            "useSpringBoot3" to "true",
            "interfaceOnly" to "true",
            "delegatePattern" to "false",
            "skipDefaultInterface" to "false",
            "useResponseEntity" to "true",
            "dateLibrary" to "java8",
            "useTags" to "true",
            "serializableModel" to "true",
            "useOptional" to "false",
            "unhandledException" to "true",
            "library" to "spring-boot",
            "hideGenerationTimestamp" to "false",
            "documentationProvider" to "none",
            "annotationLibrary" to "none",
            "useBeanValidation" to "false"
        )
    )

    globalProperties.set(
        mapOf(
            "apis" to "",
            "models" to "",
            "supportingFiles" to "ApiUtil.java"
        )
    )
}

tasks.named("compileJava") {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.test {
    useJUnitPlatform()
}
