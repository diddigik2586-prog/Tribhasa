# Test results of the final local build

Generated from the Gradle XML results of `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --rerun-tasks` (JDK 21 on Linux, Robolectric / Android 15 runtime, native graphics).

| Test class | Tests | Failed |
|---|---:|---:|
| AppFlowTest | 23 | 0 |
| CollectStressTest | 1 | 0 |
| FlowStressTest | 1 | 0 |
| FormsTest | 9 | 0 |
| LanguagesTest | 10 | 0 |
| ListStressTest | 1 | 0 |
| PdfLayoutTest | 7 | 0 |
| RepositoryTest | 8 | 0 |
| ScriptRunsTest | 7 | 0 |
| StringsTest | 7 | 0 |
| TextChunksTest | 3 | 0 |
| **Total** | **77** | **0** |

Lint: 0 errors, 29 warnings (advisory). Debug APK: built and signed with the testing key.

On-device (emulator) results are in the GitHub Actions run of the same commit: see `IMPLEMENTATION_AND_TEST_REPORT.md`, section 2.
