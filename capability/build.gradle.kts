plugins { id("meemo.android.library") }

dependencies {
    api(project(":domain"))
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(libs.hilt.android)
}
