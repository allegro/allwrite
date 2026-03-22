package pl.allegro.tech.allwrite.cli.infrastructure.os

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/**
 * This module provides interaction with operating system:
 * - reading env variables and system properties
 * - executing system commands
 * - accessing OS metadata
 * - accessing files from local allwrite distribution
 * - interacting with local Git process
 * - ...
 */
@Module
@ComponentScan
public class OperatingSystemModule
