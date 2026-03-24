# build-logic

This included build is split into several subprojects.

The purpose of every subproject is to hold a subset of convention plugins that are mostly applied together.

Ideally, modifying a convention plugin should only change classpath of the projects that apply it. Having all convention plugins in a single project would always change classpath of every project buildscript.
