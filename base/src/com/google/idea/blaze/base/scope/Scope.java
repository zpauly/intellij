/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.scope;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Helper methods to run scoped functions and operations in a scoped context. */
public final class Scope {
  private static final Logger LOG = Logger.getInstance(Scope.class);

  /** Runs a scoped function in a new root scope. */
  public static <T> T root(@NotNull ScopedFunction<T> scopedFunction) {
    return push(null, scopedFunction);
  }

  /** Runs a scoped function in a new nested scope. */
  public static <T> T push(
      @Nullable BlazeContext parentContext, @NotNull ScopedFunction<T> scopedFunction) {
    BlazeContext context = new BlazeContext(parentContext);
    try {
      return scopedFunction.execute(context);
    } catch (RuntimeException e) {
      context.setHasError();
      LOG.error(e);
      throw e;
    } finally {
      context.endScope();
    }
  }

  /** Runs a scoped operation in a new root scope. */
  public static void root(@NotNull ScopedOperation scopedOperation) {
    push(null, scopedOperation);
  }

  /** Runs a scoped operation in a new nested scope. */
  public static void push(
      @Nullable BlazeContext parentContext, @NotNull ScopedOperation scopedOperation) {
    BlazeContext context = new BlazeContext(parentContext);
    try {
      scopedOperation.execute(context);
    } catch (RuntimeException e) {
      context.setHasError();
      LOG.error(e);
      throw e;
    } finally {
      context.endScope();
    }
  }
}
