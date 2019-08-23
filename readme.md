# CacheProbe For java

This Code is aimed at projects that have need to get information about Cache in java since java does not provide this sort of information.

To know cache may be advantage when you have longer data pipeline (process whole data multiple times in step).
Then project may split data in such way it will fit mostly in cache 
(example: split data by L3 cache bytes (and divide  by available processors  if working on multiple  threads)). 

However reports may be inaccurate since some level of cache on some systems are
on just one core (L3 should be across cores most of times)
(L2 is core specific) so if you are working on same data across threads(possibly cores)
it may be better (this is speculation) to aline it against L3 (L4 on multiCPU systems)

## Usage
Class CacheProbe has public static variable ... that contains info about cache
Just look at code and you will see
 

## Tested OS
- linux (bash required)
- windows (cmd.exe required)



## Warranty
 no warranty use on your own risk

## licence
  free to use
  do not distribute your own incomparable version with same classpath/name
  
   