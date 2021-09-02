package com.github.danishjamal104.notes.util.exception

import java.lang.Exception

class UserStateException(var reason: String): Exception(reason) {
}