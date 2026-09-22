/*
 * Copyright © 2026 James Carman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jwcarman.guvnor.domain;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The desk's services, for an application that wants them.
 *
 * <p>Imported rather than scanned across. An application that widens its own component scan to
 * cover this package also replaces the filter Spring Boot installs by default -- the one that keeps
 * {@code @TestConfiguration} classes out of the application context -- and then every test's
 * private fixtures are loaded into every other test's context. That failure reads as a duplicate
 * bean definition a long way from its cause.
 */
@Configuration(proxyBeanMethods = false)
@ComponentScan
public class DomainConfiguration {}
