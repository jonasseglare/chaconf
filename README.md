# chaconf

A software to plan chamber music weekends. Here's the problem:

Chamber music weekends are organized with many participants, each participant playing an instrument. The number of participants of each instrument is known.
The participants are divided into ensembles. An ensemble typically has a configuration, e.g. two violins and one cello. The problem is to list all the possible number of ensembles that will perfectly match the participants of different instruments. For instance, given ```a```, ```b```, ```c```, ```d```, list all non-negative integer values of ```x```, ```y```, ```z```, ```s```, ```t```, ```u``` such that

```
a*Violin + b*Cello + c*Alto + d*Piano = x*(Violin + Violin) + y*(Violin + Alto) + z*(Violin + Piano) + s*(Violin + Cello + Alto) + u*(Violin + Violin + Cello + Alto)
```
For every ensemble, a teacher is paid to direct it. So generally, we would prefer a lower number of ensembles, although this is not an objective of the software.

It could be that the day is divided in several sessions, and the number of instruments of each type varies from session to session. The number of teachers, though, should be the same. In that case, the software tries to list possible configurations for every session so that number of teachers does not change.

## Usage
First, specify your problem in a text file, e.g:
```
Session avant-midi:
Violin 20
Cello 12
Alto 5
Piano 3

Session apres-midi:
Violin 22
Cello 12
Alto 4
Piano 3

Ensemble V+V:
Violin 2

Ensemble V+A:
Violin 1
Alto 1

Ensemble V+C:
Violin 1
Cello 1

Ensemble V+P:
Violin 1
Piano 1

Ensemble V+A+C:
Violin 1
Alto 1
Cello 1

Ensemble 2V+A+C:
Violin 2
Alto 1
Cello 1
```

Launch it using a file containing the specification, e.g.
```
lein run resources/config.txt
```
or
```
java -jar chaconf-0.1.0-SNAPSHOT-standalone.jar resources/config.txt
```
If you omit the filename argument, a file chooser dialog will be displayed.

Once the program is running, the file will be loaded and parsed. From the specification, all solutions are computed and displayed on a web page:
![resources/solution.png](Solution web page)

## License

Copyright © 2019 Jonas Östlund

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
