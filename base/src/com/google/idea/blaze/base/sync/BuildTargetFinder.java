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
package com.google.idea.blaze.base.sync;

import com.google.idea.blaze.base.bazel.BuildSystemProvider;
import com.google.idea.blaze.base.io.FileAttributeProvider;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.model.primitives.WorkspaceRoot;
import com.google.idea.blaze.base.settings.Blaze;
import com.google.idea.blaze.base.sync.projectview.ImportRoots;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import java.io.File;
import javax.annotation.Nullable;

/** Finds the best target to sync for a file. */
public class BuildTargetFinder {
  private final WorkspaceRoot workspaceRoot;
  private final ImportRoots importRoots;
  private final FileAttributeProvider fileAttributeProvider;
  private final BuildSystemProvider buildSystemProvider;

  public BuildTargetFinder(Project project, WorkspaceRoot workspaceRoot, ImportRoots importRoots) {
    this.workspaceRoot = workspaceRoot;
    this.importRoots = importRoots;
    this.fileAttributeProvider = FileAttributeProvider.getInstance();
    this.buildSystemProvider = Blaze.getBuildSystemProvider(project);
  }

  @Nullable
  public TargetExpression findTargetForFile(File file) {
    if (fileAttributeProvider.isFile(file)) {
      file = file.getParentFile();
      if (file == null) {
        return null;
      }
    }

    final File directory = file;
    File root =
        importRoots
            .rootDirectories()
            .stream()
            .map(workspaceRoot::fileForPath)
            .filter(potentialRoot -> FileUtil.isAncestor(potentialRoot, directory, false))
            .findFirst()
            .orElse(null);
    if (root == null) {
      return null;
    }

    File currentDirectory = directory;
    do {
      File buildFile = buildSystemProvider.findBuildFileInDirectory(currentDirectory);
      if (buildFile != null) {
        return TargetExpression.allFromPackageNonRecursive(
            workspaceRoot.workspacePathFor(currentDirectory));
      }
      currentDirectory = currentDirectory.getParentFile();
    } while (currentDirectory != null && FileUtil.isAncestor(root, currentDirectory, false));

    return null;
  }
}
