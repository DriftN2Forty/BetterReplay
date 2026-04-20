package me.justindevb.replay;

import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplayConfigSettingTest {

    @Mock private FileConfiguration config;

    @Test
    void getBoolean_usesExpectedKeyAndDefault() {
        when(config.getBoolean("General.Check-Update", true)).thenReturn(false);

        boolean value = Replay.ConfigSetting.CHECK_UPDATE.getBoolean(config);

        assertFalse(value);
        verify(config).getBoolean("General.Check-Update", true);
    }

    @Test
    void getString_usesExpectedKeyAndDefault() {
        when(config.getString("General.Storage-Type", "file")).thenReturn("mysql");

        String value = Replay.ConfigSetting.STORAGE_TYPE.getString(config);

        assertEquals("mysql", value);
        verify(config).getString("General.Storage-Type", "file");
    }

    @Test
    void getInt_usesExpectedKeyAndDefault() {
        when(config.getInt("list-page-size", 10)).thenReturn(25);

        int value = Replay.ConfigSetting.LIST_PAGE_SIZE.getInt(config);

        assertEquals(25, value);
        verify(config).getInt("list-page-size", 10);
    }

    @Test
    void applyDefault_registersSettingDefault() {
        Replay.ConfigSetting.MYSQL_PORT.applyDefault(config);

        verify(config).addDefault("General.MySQL.port", 3306);
    }
}
