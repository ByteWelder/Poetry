## 5.0.0

- AndroidX
- Android minimum SDK 21
- Android target/build SDK 29
- Android Gradle plugin 3.4.1
- Kotlin 1.3.31
- Gradle Wrapper 5.1.1
- General cleanup
- Removed deprecated `JsonPathResolver.resolvePath()`
- Removed create/release database helper method: use `OpenHelperManager` from ORMLite instead
- Removed DaoUtils
- Moved classes in more logical packages

## 4.0.0

- using OrmLite 5.0
- Package ID renamed to `poetry`
- Updated Android SDKs and dependencies to API version 25
- Updated gradle-wrapper
- Removed slf4j dependency
- added release guidelines

## 3.0.0

Improvements:
 - improved tests

Deprecations:
 - merged poetry-core and poetry-data into one library project: poetry
 - removed (unused) Callback code
 - removed (unused) service-related code

## 2.3.0 (2015-10-10)

Improvements:
 - performance improvements for big datasets with JsonPersister

## 2.2.0 (2015-10-07)

Bugfixes:
 - fix for foreign relationships
 - updated tests

## 2.1.0 (2015-09-25)

Improvements:
 - added MapFrom annotation
 
## 2.0.0 (2015-09-15)

Deprecations:
 - removed poetry-web because Android's Apache HttpClient is deprecated

## 1.2.0 (2015-09-02)

Improvements:
 - added ability to persist foreign relationships when only an ID is provided in JSON
   (rather than an object with an ID embedded in it)

## 1.1.0 (2015-06-05)

Improvements:
  - added first test
  - created demo project

Bugfixes:
  - fix for reserved table names (e.g. "Group")

Deprecations:
  - removed HttpRequestJsonPersister
  - removed MappedJsonPersister

## 1.0.0

Initial release