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
package org.jwcarman.guvnor.desk;

import org.jwcarman.guvnor.domain.DomainConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * The billing support desk, with a person doing the reading.
 *
 * <p>There is no model here and nothing automatic. That is the whole lesson: run it, send it the
 * worst email you can write, and watch nothing happen.
 */
@SpringBootApplication
@Import(DomainConfiguration.class)
public class DeskApplication {

  public static void main(String[] args) {
    SpringApplication.run(DeskApplication.class, args);
  }
}
