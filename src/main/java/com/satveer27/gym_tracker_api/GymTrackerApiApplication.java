package com.satveer27.gym_tracker_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GymTrackerApiApplication {

	public static void main(String[] args) {
        SpringApplication.run(GymTrackerApiApplication.class, args);
	}

}
