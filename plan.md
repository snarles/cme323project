# Overall plan

1. We will be using two repositories.  This repository will be used for papers, prototypes (possibly in other languages), scratch space and miscellaneous items.  Once we get our Scale code to a working version, we will decide on a project name, and then create a new repository in the format of Spark packages, and place a cleaned-up copy of our code in there.
2. I have an idea for the theoretical component of the project: it will mainly be an analysis of distributing All-Pairs-Shortest-Paths with 1) Floyd-Warshall, 2) iterated squaring, 3) some new randomized algorithms which can be viewed as a generalization of the previous two.  For more details, see the simulations folder.
3. I am using RStudio to code some prototypes.  These are purely for running simulations for the theoretical component.
4. The implementation component will be to implement distributed All-Pairs-Shortest-Paths and ISOMAP in Spark.  The first step will be to create a subclass of BlockMatrix (see below)

# Implementation

In Spark, see if there is already be an efficient way to do the following: suppose *C* is an *(n, m)* BlockMatrix, *A* is *(n, k)*, and *B* is *(k, m)* where *k* is small, then we should be able to update
```
C = C + A * B
```
without moving the data in *C*.

If so, we can implemented distributed all-pairs-shortest-paths easily given a subclass of BlockMatrix with two additional methods:

* **def min(other: BlockMatrix): BlockMatrix = ** Takes the elementwise minimum of this [[BlockMatrix]] and other [[BlockMatrix]] (they have to have the same dimension)
* **def minPlus(other: BlockMatrix): BlockMatrix = ** Multiplies this [[BlockMatrix]] and the other [[BlockMatrix]] according to min-plus multiplication.  The `colsPerBlock` of this matrix must equal the `rowsPerBlock` of `other`

