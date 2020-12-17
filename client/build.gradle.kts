group = "ru.senin.kotlin.net.client"
application.mainClassName = "io.ktor.server.netty.EngineMain"

dependencies {
    implementation(project(":shared"))
    
    implementation("com.apurebase:arkenv:3.1.0")

    val ktor_version: String by project
    val logback_version: String by project
    val retrofit_version: String by project

    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("io.ktor:ktor-network:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-json:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    
    implementation("com.squareup.retrofit2:retrofit:$retrofit_version")
    implementation("com.squareup.retrofit2:converter-jackson:$retrofit_version")
    
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}