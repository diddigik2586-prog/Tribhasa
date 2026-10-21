# Test results of the final local build (version 0.2.0)

Generated from the Gradle XML results of `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` (JDK 21 on Linux, Robolectric / Android 15 runtime, native graphics).

| Test class | Tests | Failed |
|---|---:|---:|
| AppFlowTest | 24 | 0 |
| CollectStressTest | 1 | 0 |
| DarkModeKeepsPaletteTest | 1 | 0 |
| FlowStressTest | 1 | 0 |
| FormsTest | 9 | 0 |
| HomeV2Test | 12 | 0 |
| InformationPagesTest | 5 | 0 |
| LanguagesTest | 10 | 0 |
| ListStressTest | 1 | 0 |
| NarrowScreenTest | 1 | 0 |
| PdfLayoutTest | 7 | 0 |
| PlannerExplicitEditTest | 5 | 0 |
| ProfileCancelAfterKeepEditingTest | 1 | 0 |
| ProfileFlowTest | 6 | 0 |
| RepositoryTest | 8 | 0 |
| ScriptRunsTest | 7 | 0 |
| StringsTest | 8 | 0 |
| TextChunksTest | 3 | 0 |
| ThemeAndHeroTest | 2 | 0 |
| V2ScreenshotTour | 1 | 0 |
| **Total** | **113** | **0** |

Lint: 0 errors, 29 warnings (advisory). Debug APK: built and signed with the testing key.

On-device (emulator) results are in the GitHub Actions run of the same code: see `IMPLEMENTATION_AND_TEST_REPORT.md`, section 2.
