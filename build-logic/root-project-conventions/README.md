# root-project-conventions

Contains convention plugins to be applied in root project.

Due to being applied to root project, every source file in `root-project-conventions` will land on every buildscript classpath in the entire build (subprojects inherit classpath from the parent project).

As a result, modifying any file within `root-project-convention` will trigger full build reconfiguration and all tasks will go out of date (except for the built-in ones).

When to put a convention plugin here:

- it changes very rarely,
- it performs some heavy operation that you want to keep UP-TO-DATE most of the time (like fetching JDKs),
- it must be applied to root project for technical reasons
