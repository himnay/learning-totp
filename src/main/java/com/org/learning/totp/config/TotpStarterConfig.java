package com.org.learning.totp.config;

import dev.samstevens.totp.spring.autoconfigure.TotpAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@code totp-spring-boot-starter} 1.7.1 (the latest release on Maven Central, last published
 * 2020-11-05) registers its auto-configuration exclusively via the legacy
 * {@code META-INF/spring.factories} mechanism. Spring Boot 3+ dropped support for that file for
 * auto-configuration classes — only {@code META-INF/spring/...AutoConfiguration.imports} is
 * honored — so on a modern Spring Boot version the starter's beans are silently never created if
 * you rely on classpath scanning alone.
 *
 * <p>Importing {@link TotpAutoConfiguration} directly sidesteps that: it's an ordinary
 * {@code @Configuration} class, so {@code @Import} registers its {@code @Bean} methods and its
 * {@code @EnableConfigurationProperties(TotpProperties.class)} exactly as if the starter had been
 * auto-configured, without forking or shading the library.
 */
@Configuration
@Import(TotpAutoConfiguration.class)
public class TotpStarterConfig {}
