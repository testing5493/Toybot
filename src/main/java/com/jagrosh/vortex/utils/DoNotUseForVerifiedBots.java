package com.jagrosh.vortex.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks code that should not be used for verified bots.
 * Using any code marked as such for verified bots may compromise application stability
 */
@Target(ElementType.TYPE_USE)
public @interface DoNotUseForVerifiedBots {}
