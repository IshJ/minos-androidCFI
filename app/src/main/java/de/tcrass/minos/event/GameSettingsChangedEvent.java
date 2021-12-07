package de.tcrass.minos.event;

import de.tcrass.minos.model.SettingsFormData;

public class GameSettingsChangedEvent {

    private SettingsFormData gameSettings;

    public GameSettingsChangedEvent(SettingsFormData gameSettings) {
        this.gameSettings = gameSettings;
    }

    public SettingsFormData getGameSettings() {
        return gameSettings;
    }

}
