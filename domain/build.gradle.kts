plugins { id("meemo.kotlin.library") }

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}
