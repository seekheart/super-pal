package com.seekheart.superpal.util

import com.uchuhimo.konf.Config
import com.uchuhimo.konf.ConfigSpec

fun getConfig(configType: ConfigSpec): Config {
    return Config {
        addSpec(configType)
    }.from.env()
}