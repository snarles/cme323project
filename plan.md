# Overall plan

* ISOMAP with all-pairs-shortest-paths
* Approximate ISOMAP using column sampling

# Logistics

* We will be using two repositories.  This repository will be used for papers, prototypes (possibly in other languages), scratch space and miscellaneous items.  Once we get our Scale code to a working version, we will decide on a project name, and then create a new repository in the format of Spark packages, and place a cleaned-up copy of our code in there.


## ISOMAP with APSP

1. I have an idea for the theoretical component of the project: it will mainly be an analysis of distributing All-Pairs-Shortest-Paths with 1) Floyd-Warshall, 2) iterated squaring, 3) some new randomized algorithms which can be viewed as a generalization of the previous two.  For more details, see the simulations folder.
2. I am using RStudio to code some prototypes.  These are purely for running simulations for the theoretical component.
3. The implementation component will be to implement distributed All-Pairs-Shortest-Paths and ISOMAP in Spark.  The first step will be to create a subclass of BlockMatrix (see below)


### Implementation

In Spark, see if there is already be an efficient way to do the following: suppose *C* is an *(n, m)* BlockMatrix, *A* is *(n, k)*, and *B* is *(k, m)* where *k* is small, then we should be able to update
```
C = C + A * B
```
without moving the data in *C*.

If so, we can implemented distributed all-pairs-shortest-paths easily given a subclass of BlockMatrix with two additional methods:

* ```def min(other: BlockMatrix): BlockMatrix = ``` Takes the elementwise minimum of this [[BlockMatrix]] and other [[BlockMatrix]] (they have to have the same dimension)
* ```def minPlus(other: BlockMatrix): BlockMatrix = ``` Multiplies this [[BlockMatrix]] and the other [[BlockMatrix]] according to min-plus multiplication.  The `colsPerBlock` of this matrix must equal the `rowsPerBlock` of `other`

# Approximate ISOMAP using column sampling

* In this alternative approach we would use PREGEL to store the initial k-nearest-neighbor distances.  (We might use locality-sensitive-hashing to find the nearest neighbors)
* Rather than form the entire APSP distance matrix D, compute a fixed number of columns of D by using single-source shortest-paths for a randomly selected vertex
* Computing SVD on the centered submatrix of D will approximate the ISOMAP coordinates.  See reference on [randomized SVD](http://simons.berkeley.edu/sites/default/files/docs/627/drineasslides.pdf)
