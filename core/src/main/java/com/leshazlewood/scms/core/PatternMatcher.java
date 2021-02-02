/*
 * Copyright 2013 Les Hazlewood, scms contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.leshazlewood.scms.core;

/**
 * Interface for components that can match source strings against a specified pattern string.
 *
 * <p>Different implementations can support different pattern types, for example, Ant style path
 * expressions, or regular expressions, or other types of text based patterns.
 *
 * <p>This idea was ganked from the <a href="http://shiro.apache.org">Apache Shiro</a> project (with
 * much appreciation).
 *
 * @see AntPathMatcher AntPathMatcher
 * @since 0.1
 */
public interface PatternMatcher {

  /**
   * Returns {@code true} if the given {@code source} matches the specified {@code pattern}, {@code
   * false} otherwise.
   *
   * @param pattern the pattern to match against
   * @param source the source to match
   * @return {@code true} if the given {@code source} matches the specified {@code pattern}, {@code
   *     false} otherwise.
   */
  boolean matches(String pattern, String source);
}
