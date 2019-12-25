package com.seekheart.superpal.config

import com.uchuhimo.konf.ConfigSpec

object FeignConfig : ConfigSpec("feign") {
    val user by required<String>()
    val password by required<String>()
}