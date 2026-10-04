# Isolated local backup restore check

`tools/Test-LocalBackup.ps1` checks a clean stopped PostgreSQL 16 development cluster copy together with its matching `MatchCats-server.jar`. It copies the cluster into a new scratch directory, starts localhost PostgreSQL on an unused port, runs read-only SQL checks and starts the paired application on another unused port. It stops both processes and restores process environment settings when finished. Scratch logs/data remain for inspection; remove the scratch database only after confirming it has stopped.

This is a development verification tool, not a production backup system. It expects the local `matchcats_dev` database, the development `postgres` account with local trust authentication, a self-contained PostgreSQL configuration and no linked files/tablespaces. Do not expose these settings publicly or apply them to a production database.

## Inputs

Run in PowerShell, supplying actual local paths:

```powershell
.\tools\Test-LocalBackup.ps1 `
  -WorkspaceDirectory '<workspace>' `
  -BackupDirectory '<workspace>\backups\<stopped-cluster-snapshot>' `
  -ScratchDirectory '<workspace>\restore-check-new' `
  -PostgreSQLBin '<PostgreSQL 16 bin directory>' `
  -JavaExecutable '<Java 21 java.exe>' `
  -JdbcJar '<PostgreSQL JDBC driver jar>'
```

Backup and scratch must both be inside the specified workspace. Scratch must not already exist, overlap the backup or contain the backup. The backup must contain `pgdata/PG_VERSION` with version 16 and `MatchCats-server.jar`. Default ports are 55433 and 8086; occupied ports cause rejection. It does not overwrite the current development cluster or stop unrelated application processes. No user login/password or private database content is printed; SQL reports row counts and schema version.

Always pair a database snapshot with the application version saved at that time. Starting a newer application may migrate the restored copy, which is a separate upgrade test; starting an older application against a newer schema may fail validation. A live directory copy without an appropriate PostgreSQL backup procedure is not a reliable snapshot.

## Verified locally on 2026-10-04

A snapshot made before email-verification deployment restored successfully. Its development schema was V12, with two accounts and empty cat, photo, conversation, message, document, proposal and receipt tables. SQL reads and schema checks passed, and the saved application started with its interface and health endpoint available. The temporary database/application were then stopped; the running V13 development application remained available.

These checks establish startup/readability for that snapshot. They do not establish a production recovery-time objective, point-in-time recovery, encrypted offsite storage, full row-by-row equivalence, or recovery of populated photos/documents. A representative populated-data restore test and an actual production backup/retention/access policy remain required before public deployment.
