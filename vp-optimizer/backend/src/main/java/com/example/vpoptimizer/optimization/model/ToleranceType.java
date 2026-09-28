package com.example.vpoptimizer.optimization.model;

/** How the user expressed the acceptable VP tolerance. */
public enum ToleranceType {

    /** Tolerance is a percentage of the target VP (10 means +/- 10%). */
    PERCENTAGE,

    /** Tolerance is an absolute VP amount (50 means +/- 50 VP). */
    ABSOLUTE
}
