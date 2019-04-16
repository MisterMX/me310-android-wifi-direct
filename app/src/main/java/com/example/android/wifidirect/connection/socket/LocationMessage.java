package com.example.android.wifidirect.connection.socket;

import android.location.Location;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LocationMessage {
    private String deviceId;
    private double longitude;
    private double latitude;

    public Location toLocation() {
        Location location = new Location(deviceId);
        location.setLongitude(longitude);
        location.setLatitude(latitude);

        return location;
    }
}
