# Releasing Poetry

1. Check if version is correct in `build.gradle`
2. Run tests
3. Release: `./gradlew clean build bintrayUpload -PdryRun=false -PbintrayUser=??? -PbintrayKey=???`
4. Push version tag to git
5. Bump version in repository