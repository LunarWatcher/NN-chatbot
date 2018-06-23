package io.github.lunarwatcher.chatbot

import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AnonymousLogger")

fun String.info() = this.info(logger);
fun String.debug() = this.debug(logger);
fun String.error() = this.error(logger);
fun String.trace() = this.trace(logger);
fun String.warn() = this.warn(logger);

fun String.info(logger: Logger) = logger.info(this);
fun String.debug(logger: Logger) = logger.debug(this);
fun String.error(logger: Logger) = logger.error(this)
fun String.trace(logger: Logger) = logger.trace(this);
fun String.warn(logger: Logger) = logger.warn(this)
