package com.example.vpoptimizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Product &amp; Volume Point optimization backend.
 *
 * <p>Given the products a user selects, a discount, a GST rate, a target VP and a tolerance, the
 * optimization engine returns the best purchase combinations by VP proximity and final payable
 * cost.</p>
 */
@SpringBootApplication
public class VpOptimizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(VpOptimizerApplication.class, args);
    }
}
