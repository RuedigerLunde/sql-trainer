# How to set up the project #

If you are a Java developer, you might want to create a customized version of the software. You can get everything needed via the ZIP download or by cloning of the git repository. In both cases, the resulting file structure should look as follows:

```
<MyEclipseOrOtherIDESQLTrainerProjectBaseDirectory> (e.g. 'SQLTrainer')
   bin (contains class files after compilation)
   doc (contains javadoc source code documentation)
   sql-trainer-config (*)
      dbinfo (contains infos about training DBs)
      exercises (contains one example exercise set)
      lib (contains jdbc drivers)
      xml (contains dtds an xslt files)
      static-properties.xml (contains default settings)
   src (contains java source)
   wiki (contains documents about the SQLTrainer)
   SQLTrainer.jar (runnable jar to start the SQLTrainer) (**)
```

When executing the program, make sure that the sql-trainer-config directory is in the current directory of the execution environment. To just run the SQLTrainer, the complete subdirectory `(*)` and the jar-file `(**)` are sufficient.

For compilation, the build path must be set correctly. All source code is located in the src folder and additional jar libraries from the lib folder are needed. For Eclipse users, it is recommended to just import the whole project.
SVN users might remember, that '.svn' subdirectories must be excluded from the source path (exclude syntax in Eclipse: `**/.svn/*`).

The main method to start the application is in class
`rl.sqltrainer.gui.SQLTrainerStarter`.
