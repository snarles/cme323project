/** Simple Block-APSP test (Spark)
  * Charles Zheng
  *
  * A test of a very minimalistic version of Block-APSP
  * Demonstrates running time and checks exactness of Block-APSP
  * Tests the algorithm on a randomly shuffled 1D, 2D, or 3D lattice graph
  */

import scala.util._
import scala.math._
import org.apache.spark.HashPartitioner
import org.apache.spark.mllib.random.UniformGenerator

sc.setCheckpointDir("checkpoint/")

/**
 * Problem parameters
 */

//Choose the dimension of the lattice (1, 2, 3)
val dim = 1
//Set randomness
val seed = 2

// Size of the problem and how data is stored (blocks)
val blocksize = 6
val nblocks = 2
val pparts = nblocks * nblocks
val n = blocksize * nblocks

/**
 * Choose the sub-block size (for Block FW).
 * THIS HAS TO DIVIDE THE BLOCK SIZE!
 * Set subsize = 1 for Original Floyd-Warshall
 */

val subsize = 3

/**
 * Shuffle vertices psuedorandomly
 */
val r = new UniformGenerator()
r.setSeed(seed)
//val ids = (for (i <- 0 to n-1) yield(r.nextValue())).zipWithIndex.sortBy(_._1).map(_._2)
val ids = List.range(0, n)

/**
 * Functions for defining the adjacency matrix
 */

// Infinity, to be used to indicate abscence of an edge
val inf = scala.Double.PositiveInfinity

// edge length of graph
val width = ceil(pow(n.toFloat, 1/(dim.toFloat))).toInt
val divs = Vector.range(0, dim).map(i => pow(width, i).toInt).reverse

// d-Dimensional lattice coordinates of each point
def coords(x: Int): Vector[Int] = {
  Vector.range(0, dim).map(i => (ids(x)/divs(i)) % width)
}

// Adjacency matrix
def dist(x: Int, y:Int): Double = {
  val diff = (for (i <- 0 to dim - 1) yield(abs(coords(x)(i) - coords(y)(i)))).sum
  diff match {
    case 0 => 0.0
    case 1 => 1.0
    case -1 => 1.0
    case _ => inf
  }
}

// Shortest path distance function between two vertices (L1 distance of coordinates)
def trueDist(x: Int, y: Int): Double = (for (i <- 0 to dim - 1) yield(abs(coords(x)(i) - coords(y)(i)))).sum

/**
 * Function for building the block of the adjacency matrix from the block indices
 * The matrix blocks are stored as DENSE lists of ((i, j), w_ij) tuples
 * Hence each block has blocksize^2 elements to start with
 * (This is inefficient, of course)
 */

def buildMatrix(x : (Int, Int)): ((Int, Int), List[((Int, Int), Double)]) = {
  val inds1 = List.range(x._1 * blocksize, (x._1+1) * blocksize)
  val inds2 = List.range(x._2 * blocksize, (x._2+1) * blocksize)
  val p = for (x <- inds1; y <- inds2) yield (x, y)
  (x, p.map{x => (x, dist(x._1, x._2))})
}

//Function for building the block of the correct APSP matrix from the block indices
def buildTrueMatrix(x : (Int, Int)): ((Int, Int), List[((Int, Int), Double)]) = {
  val inds1 = List.range(x._1 * blocksize, (x._1+1) * blocksize)
  val inds2 = List.range(x._2 * blocksize, (x._2+1) * blocksize)
  val p = for (x <- inds1; y <- inds2) yield (x, y)
  (x, p.map{x => (x, trueDist(x._1, x._2))})
}

/**
 * Initialize the RDDs with the initial adjacency matrix and the final APSP matrix
 */

// Number of partitions used by Spark
val npartitions = min(pparts, 48)

// A list of BLOCK indices (there are p of them)
val blockids = List.range(0, pparts).map{x => (x / nblocks, x % nblocks)}

// Partitioner to be used by the adjency matrix
val part = new HashPartitioner(npartitions)

// Blocks RDD
val blocks = sc.parallelize(blockids, npartitions).map(buildMatrix).partitionBy(part)

// An RDD holding the correct answer
val groundTruth = sc.parallelize(blockids, npartitions).map(buildTrueMatrix).flatMap(_._2.toList)

/**
 * Functions used in block-FW
 * k = sub-block number, from 0 to (n/subsize) - 1
 */


// Extracts the elements of the matrix with both indices in ((k-1) * subsize) to ((k * subsize) - 1)
def extractSquare(k: Int)(x: List[((Int, Int), Double)]) : List[((Int, Int), Double)] = {
  x.filter(v => (v._1._1/subsize == k  && v._1._2/subsize == k))
}

// Extracts the rows of the matrix with indices ((k-1) * subsize) to ((k * subsize) - 1)
def extractRowSlice(k : Int)(x: List[((Int, Int), Double)]) : List[((Int, Int), Double)] = {
  x.filter(v => (v._1._1/subsize == k))
}

// Same as above but for columns
def extractColSlice(k : Int)(x: List[((Int, Int), Double)]) : List[((Int, Int), Double)] = {
  x.filter(v => (v._1._2/subsize == k))
}

// Updates the row slice using the square matrix
def updateRowSlice(k: Int, x: List[((Int, Int), Double)])(y: List[((Int, Int), Double)]) : List[((Int, Int), Double)] = {
  val xm = x.map(x => x._1 -> x._2).toMap
  val ym = y.map(x => x._1 -> x._2).toMap
  val matrix = y
  for (v <- matrix) yield {
    val i = v._1._1
    val j = v._1._2
    var wij = v._2
    for (l <- 0 to subsize - 1) {
      wij = min(wij, xm((i, k * subsize + l)) + ym((k * subsize + l, j)))
    }
    (v._1, wij)
  }
}

// Updates the column slice using the square matrix
def updateColSlice(k: Int, x: List[((Int, Int), Double)])(y: List[((Int, Int), Double)]) : List[((Int, Int), Double)] = {
  val xm = x.map(x => x._1 -> x._2).toMap
  val ym = y.map(x => x._1 -> x._2).toMap
  val matrix = y
  for (v <- matrix) yield {
    val i = v._1._1
    val j = v._1._2
    var wij = v._2
    for (l <- 0 to subsize - 1) {
      wij = min(wij, ym((i, k * subsize + l)) + xm((k * subsize + l, j)))
    }
    (v._1, wij)
  }
}


// Makes copies of each piece of each row for every block in the column
def replicateRow(x: ((Int, Int), List[((Int, Int), Double)])): List[((Int, Int), List[((Int, Int), Double)])] = {
  for (i <- List.range(0, nblocks)) yield((i, x._1._2), x._2)
}

// Same as above but for columns
def replicateCol(x: ((Int, Int), List[((Int, Int), Double)])): List[((Int, Int), List[((Int, Int), Double)])] = {
  for (i <- List.range(0, nblocks)) yield((x._1._1, i), x._2)
}


// Local FW for a matrix of size l
def localFW(k: Int)(x: List[((Int, Int), Double)]) : List[((Int, Int), Double)] = {
  var m = collection.mutable.Map() ++ x.map(x => x._1 -> x._2).toMap
  for (kk <- (k * subsize) to ((k + 1) * subsize - 1)) {
    for (ii <- (k * subsize) to ((k + 1) * subsize - 1)) {
      for (jj <- (k * subsize) to ((k + 1) * subsize - 1)) {
        m((ii, jj)) = min(m(ii, jj), m(ii, kk) + m(kk, jj))
      }
    }
  }
  m.toList
}


// The block-FW iteration
def update(k : Int)(x: (List[((Int, Int), Double)], List[((Int, Int), Double)])) : List[((Int, Int), Double)] = {
  val matrix = x._1
  val subblock = x._2
  val m = subblock.map(x => x._1 -> x._2).toMap
  for (v <- matrix) yield {
    val i = v._1._1
    val j = v._1._2
    var wij = v._2
    for (l <- 0 to subsize - 1) {
      wij = min(wij, m((i, k * subsize + l)) + m((k * subsize + l, j)))
    }
    (v._1, wij)
  }
}

/**
 * Sets up the DAG for the Spark Job
 * Details: To avoid issues related to recursive updated,
 *   EACH iterate of the RDD are stored separately
 * Instead of blocks = blocks.update
 *   we have blocks[k+1] = blocks[k].update
 * This could potentially lead to issues??
 */

val niters = n/subsize
// The updated RDD per iteration is stored as an element of a list
var allblocks = List(blocks)

blocks.count
val time1 : Long = System.currentTimeMillis
// The n/subsize iterations of Block-FW
for (k <- 0 to niters-1) {
  println(k)
  val blockind = k*subsize/blocksize
  val exSquare0 = allblocks(k).filter(x => (x._1._1== blockind && x._1._2 == blockind)).mapValues(extractSquare(k)).collect()
  val exSquare = exSquare0(0)._2
  val x = localFW(k)(exSquare)
  val exRow = allblocks(k).filter(x => (x._1._1== blockind)).mapValues(extractRowSlice(k))
  val exRow2 = exRow.mapValues(updateRowSlice(k, x))
  val exCol = allblocks(k).filter(x => (x._1._2== blockind)).mapValues(extractColSlice(k))
  val exCol2 = exCol.mapValues(updateColSlice(k, x))
  val dupRow = exRow2.flatMap(replicateRow)
  val dupCol = exCol2.flatMap(replicateCol)
  val dups = dupRow.join(dupCol, part).mapValues(x => x._1.union(x._2))
  val newblocks = allblocks(k).join(dups, part).mapValues(update(k))
  allblocks = allblocks :+ newblocks
}
// The final updated RDD
val fblocks = allblocks(niters)

fblocks.count
val time2 : Long = System.currentTimeMillis
// Extract the individual entries as an RDD
val elements = fblocks.flatMap(_._2.toList)
// Join with the correct APSP matrix for comparison
val compare = elements.join(groundTruth)

/**
 * Kicks off the calculation and records the time
 */

val errorsRDD = compare.filter(x => x._2._1 != x._2._2)
val errors = errorsRDD.count


/**
 * Check the results
 */

val time : Double = (time2 - time1).toFloat / 1000
val percent_error : Double = errors.toFloat/(n * n)
