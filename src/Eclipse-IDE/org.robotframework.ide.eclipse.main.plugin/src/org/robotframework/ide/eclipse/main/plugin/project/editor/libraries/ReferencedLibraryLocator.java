/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.editor.libraries;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.core.runtime.Path;
import org.rf.ide.core.environment.EnvironmentSearchPaths;
import org.rf.ide.core.environment.IRuntimeEnvironment.RuntimeEnvironmentException;
import org.rf.ide.core.project.ImportPath;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.project.RobotProjectConfig.LibraryType;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.RedWorkspace;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProjectPathsProvider;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.project.RedEclipseProjectConfig;

import com.google.common.base.Objects;

public class ReferencedLibraryLocator {

    private final RobotProject robotProject;

    private final IReferencedLibraryImporter importer;

    private final IReferencedLibraryDetector detector;

    private final RobotProjectConfig projectConfig;

    private final Map<LibImportCacheKey, Collection<ReferencedLibrary>> libImportCache = new HashMap<>();

    public ReferencedLibraryLocator(final RobotProject robotProject, final IReferencedLibraryImporter importer,
            final IReferencedLibraryDetector detector) {
        this.robotProject = robotProject;
        this.importer = importer;
        this.detector = detector;
        this.projectConfig = robotProject.getRobotProjectConfig();
    }

    public void locateByName(final RobotSuiteFile suiteFile, final String name) {
        Optional<File> libraryFile = Optional.empty();
        try {
            libraryFile = findLibraryFileByName(suiteFile, name);
            if (libraryFile.isPresent()) {
                final Entry<File, Collection<ReferencedLibrary>> imported = importByName(libraryFile.get(), name);
                detector.libraryDetectedByName(name, imported.getKey(), imported.getValue());
            } else {
                detector.libraryDetectingByNameFailed(name, libraryFile, "Unable to find '" + name + "' library.");
            }
        } catch (final RuntimeEnvironmentException e) {
            detector.libraryDetectingByNameFailed(name, libraryFile, e.getMessage());
        }
    }

    private Optional<File> findLibraryFileByName(final RobotSuiteFile suiteFile, final String name) {
        final String currentFileDirectoryPath = suiteFile.getFile().getParent().getLocation().toOSString();
        final EnvironmentSearchPaths searchPaths = new RedEclipseProjectConfig(robotProject.getProject(), projectConfig)
                .createAdditionalEnvironmentSearchPaths();
        searchPaths.addPythonPath(currentFileDirectoryPath);
        searchPaths.addClassPath(currentFileDirectoryPath);
        return robotProject.getRuntimeEnvironment().getModulePath(name, searchPaths);
    }

    public void locateByPath(final RobotSuiteFile suiteFile, final String path) {
        Optional<File> libraryFile = Optional.empty();
        try {
            if (new Path(path).isAbsolute() || path.endsWith("/") || path.endsWith(".py")) {
                libraryFile = new RobotProjectPathsProvider(suiteFile.getRobotProject())
                        .findAbsoluteUri(suiteFile.getFile(), ImportPath.from(path))
                        .map(File::new)
                        .map(this::tryToCanonical)
                        .filter(File::exists);
                if (libraryFile.isPresent()) {
                    final Entry<File, Collection<ReferencedLibrary>> imported = importByPath(libraryFile.get());
                    detector.libraryDetectedByPath(path, imported.getKey(), imported.getValue());
                } else {
                    detector.libraryDetectingByPathFailed(path, libraryFile,
                            "Unable to find library under '" + path + "' location.");
                }
            } else {
                detector.libraryDetectingByPathFailed(path, libraryFile,
                        "The path '" + path + "' should point to either .py file or python module directory.");
            }
        } catch (final RuntimeEnvironmentException | URISyntaxException e) {
            detector.libraryDetectingByPathFailed(path, libraryFile, e.getMessage());
        }
    }

    private File tryToCanonical(final File file) {
        try {
            return file.getCanonicalFile();
        } catch (final IOException e) {
            return file;
        }
    }

    private Entry<File, Collection<ReferencedLibrary>> importByName(final File library, final String name) {
        if (library.getName().toLowerCase().endsWith(".jar")) {
            return importJavaLibrary(library, name);
        } else if (isPythonModule(library)) {
            return importPythonModuleLibrary(new File(library, "__init__.py"));
        } else {
            return importPythonLibrary(library, name);
        }
    }

    private Entry<File, Collection<ReferencedLibrary>> importByPath(final File libraryFile) {
        if (isPythonModule(libraryFile)) {
            return importPythonModuleLibrary(new File(libraryFile, "__init__.py"));
        } else {
            return importPythonLibrary(libraryFile);
        }
    }

    private boolean isPythonModule(final File libraryFile) {
        return libraryFile.isDirectory() && new File(libraryFile, "__init__.py").exists();
    }

    private Entry<File, Collection<ReferencedLibrary>> importJavaLibrary(final File library, final String name) {
        return importLibsFromFileWithCaching(new LibImportCacheKey(library, name),
                () -> importer.importJavaLib(robotProject.getRuntimeEnvironment(), robotProject.getProject(),
                        projectConfig, library, name));
    }

    private Entry<File, Collection<ReferencedLibrary>> importPythonLibrary(final File library, final String name) {
        return importLibsFromFileWithCaching(new LibImportCacheKey(library, name),
                () -> importer.importPythonLib(robotProject.getRuntimeEnvironment(), robotProject.getProject(),
                        projectConfig, library, name));
    }

    private Entry<File, Collection<ReferencedLibrary>> importPythonLibrary(final File library) {
        return importLibsFromFileWithCaching(new LibImportCacheKey(library, null),
                () -> importer.importPythonLib(robotProject.getRuntimeEnvironment(), robotProject.getProject(),
                        projectConfig, library));
    }

    private Entry<File, Collection<ReferencedLibrary>> importPythonModuleLibrary(final File libraryFile) {

        return importLibsFromFileWithCaching(new LibImportCacheKey(libraryFile, null),
                () -> newArrayList(ReferencedLibrary.create(LibraryType.PYTHON, libraryFile.getParentFile().getName(),
                        RedWorkspace.Paths.toWorkspaceRelativeIfPossible(new Path(libraryFile.getAbsolutePath()))
                                .toPortableString())));
    }

    private Entry<File, Collection<ReferencedLibrary>> importLibsFromFileWithCaching(final LibImportCacheKey key,
            final Supplier<Collection<ReferencedLibrary>> importLibrarySupplier) {
        if (!libImportCache.containsKey(key)) {
            libImportCache.put(key, importLibrarySupplier.get());
        }
        return new SimpleImmutableEntry<>(key.libraryFile, libImportCache.get(key));
    }

    public interface IReferencedLibraryDetector {

        void libraryDetectedByName(String name, File libraryFile, Collection<ReferencedLibrary> referenceLibraries);

        void libraryDetectedByPath(String path, File libraryFile, Collection<ReferencedLibrary> referenceLibraries);

        void libraryDetectingByNameFailed(String name, Optional<File> libraryFile, String failReason);

        void libraryDetectingByPathFailed(String path, Optional<File> libraryFile, String failReason);
    }

    private static class LibImportCacheKey {

        private final File libraryFile;

        private final String libraryName;

        private LibImportCacheKey(final File libraryFile, final String libraryName) {
            this.libraryFile = libraryFile;
            this.libraryName = libraryName;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == this) {
                return true;
            } else if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            } else {
                final LibImportCacheKey that = (LibImportCacheKey) obj;
                return Objects.equal(this.libraryFile, that.libraryFile)
                        && Objects.equal(this.libraryName, that.libraryName);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(libraryFile, libraryName);
        }
    }
}
