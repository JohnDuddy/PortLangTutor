package com.duddy.portugues.data.repository

import java.io.IOException

class BackendRequestException(
    val statusCode: Int,
    val backendMessage: String,
) : IOException(backendMessage)
