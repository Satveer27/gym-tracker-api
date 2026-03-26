package com.satveer27.gym_tracker_api.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    USER,
    TRAINER,
    ADMIN;

    @JsonCreator
    public static Role fromString(String result){
        return Role.valueOf(result.toUpperCase());
    }
}
