plugins { id("meemo.android.library") }

dependencies {
    api(project(":capability"))
    implementation(project(":domain"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
