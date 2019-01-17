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


## License

Copyright © 2019 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
