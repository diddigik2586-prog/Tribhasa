# Baseline for the upgrade check

`Sodhamitra-0.1.0-testing.apk` is the first testing build (version 0.1.0, the "V1" app), signed with the same committed TESTING key
as every later testing build. The CI emulator job installs it, gives it a database (made by `../make_v1_database.py` from the
exported Room schema version 1), installs the newest build **over** it and checks that the data is still there and is shown by the
new screens. It is kept here only for that check.
