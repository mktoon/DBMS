package com.micahtoo.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Clock;

@SpringBootApplication
public class HospitalApplication {
    public static void main(String[] args) { SpringApplication.run(HospitalApplication.class, args); }
    @Bean Clock clock() { return Clock.systemUTC(); }
}
