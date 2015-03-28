# SQLTrainer #

The SQLTrainer is designed to support database lectures with a tool for SQL training. The focus lies on SELECT statements. The tool can be used in two modes.

  * Simple DB client mode: You can type in some SELECT statements and execute them.

  * Environment for editing and working on exercise sets: You can create exercises with questions and answers, save them as XML file, and load them again. Solutions can be secured by password and a feedback can be provided to the user which tells the user, whether her/his statements produces the same output as the specified solutions.

![https://github.com/RuedigerLunde/sql-trainer/blob/master/sql-trainer/wiki/SQLTrainer.jpg](https://github.com/RuedigerLunde/sql-trainer/blob/master/sql-trainer/wiki/SQLTrainer.jpg)

Currently, the software is ready for use just for a special lecture at the University of Applied Sciences in Ulm. But it is quite easy, to adapt it to other environments. All you have to do is

  * modifying the static properties file,
  * add some text files in the dbinfo directory, and
  * add if needed a suitable JDBC driver to the lib directory.

To get a running development environment, some help can be found in HowToSetUpTheProject.
