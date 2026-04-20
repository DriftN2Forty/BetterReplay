# Config System Refactor Proposal

## Current Approach

The plugin registers all config defaults programmatically in `Replay.initGeneralConfigSettings()`:

```java
private void initConfig() {
    initGeneralConfigSettings();
    getConfig().options().copyDefaults(true);
    saveConfig();
}

private void initGeneralConfigSettings() {
    FileConfiguration config = getConfig();
    config.addDefault("General.Check-Update", true);
    config.addDefault("General.Compress-Replays", true);
    config.addDefault("General.Storage-Type", "file");
    config.addDefault("General.MySQL.host", "host");
    config.addDefault("General.MySQL.port", 3306);
    config.addDefault("General.MySQL.database", "database");
    config.addDefault("General.MySQL.user", "username");
    config.addDefault("General.MySQL.password", "password");
}
```

### How it works

1. `addDefault()` registers default values in memory
2. `copyDefaults(true)` merges missing keys into the loaded config
3. `saveConfig()` writes the merged result to `plugins/BetterReplay/config.yml`

### Current read sites

| File | Key | Fallback |
|------|-----|----------|
| `Replay.java` | `General.Check-Update` | *(none — relies on addDefault)* |
| `Replay.java` | `General.Storage-Type` | *(none — relies on addDefault)* |
| `FileReplayStorage.java` | `General.Compress-Replays` | `true` |
| `MySQLReplayStorage.java` | `General.Compress-Replays` | `true` |
| `ReplayCommand.java` | `list-page-size` | `10` |

### Problems

- **No comments in config.yml.** SnakeYAML strips all comments when serializing. New keys appear in the file with zero context for the server admin.
- **addDefault + copyDefaults is fragile.** Readers that don't pass an explicit fallback silently get the in-memory default but if `addDefault()` is ever removed, they return `false`/`null`/`0` with no warning.
- **Key strings are scattered.** Config keys are bare strings duplicated between the write site (`addDefault`) and every read site, with no compile-time safety.

---

## Proposed Approach

### 1. Ship a commented `config.yml` resource

Create `src/main/resources/config.yml` with all defaults and inline comments:

```yaml
# ===========================================
#        BetterReplay Configuration
# ===========================================

General:
  # Check for plugin updates on startup
  Check-Update: true

  # GZIP compress replay data files to save disk space
  Compress-Replays: true

  # Storage backend: "file" or "mysql"
  Storage-Type: file

  # MySQL connection settings (only used when Storage-Type is "mysql")
  MySQL:
    host: localhost
    port: 3306
    database: betterreplay
    user: username
    password: password

Playback:
  # Amount to change playback speed per click (0.2 = 20% steps)
  # Minimum effective speed is one step above zero
  Speed-Step: 0.2

  # Maximum playback speed multiplier (1.0 = real-time)
  Max-Speed: 1.0
```

### 2. Use `saveDefaultConfig()` instead of `copyDefaults` + `saveConfig`

```java
private void initConfig() {
    saveDefaultConfig(); // copies resource config.yml only if file doesn't exist
}
```

- First install: the commented resource file is copied verbatim — comments preserved.
- Existing installs: the file is untouched. New keys added in updates use coded fallbacks.

### 3. Always pass explicit fallbacks at read sites

```java
// Before (fragile — depends on addDefault having been called)
getConfig().getBoolean("General.Check-Update")

// After (self-contained — works even if key is missing from file)
getConfig().getBoolean("General.Check-Update", true)
```

Every `getConfig().getXxx()` call should include the default value inline so it is never dependent on `addDefault()` having run first.

### 4. Remove `initGeneralConfigSettings()`

With the resource file providing first-run defaults and all read sites carrying explicit fallbacks, the `addDefault()` calls become redundant and can be deleted entirely.

---

## Migration Impact

| Concern | Impact |
|---------|--------|
| Existing servers | No change — their `config.yml` is not overwritten by `saveDefaultConfig()` |
| New servers | Get a fully commented config file on first startup |
| New config keys in updates | Work silently via coded fallbacks; admins can copy from the resource file or docs to customize |
| Code safety | Every read has an explicit default — no silent nulls or zeros |

### What changes

| File | Change |
|------|--------|
| `src/main/resources/config.yml` | **New** — commented default config |
| `Replay.java` | Replace `initConfig()` body with `saveDefaultConfig()`, delete `initGeneralConfigSettings()` |
| `Replay.java` | Add fallback to `General.Check-Update` read |
| `Replay.java` | Add fallback to `General.Storage-Type` reads |
| `ReplaySession.java` | *(already uses fallbacks — no change needed)* |
| `FileReplayStorage.java` | *(already uses fallback — no change needed)* |
| `MySQLReplayStorage.java` | *(already uses fallback — no change needed)* |
| `ReplayCommand.java` | *(already uses fallback — no change needed)* |
