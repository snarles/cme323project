// [CHARLES] Interactive port of spark-all-pairs-shortest-path
// [CHARLES] Mainly replaces GridPartitioner2

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.mllib.linalg.{SparseMatrix, DenseMatrix, Matrix}
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, MatrixEntry, BlockMatrix}
import org.apache.spark.rdd.RDD
import breeze.linalg.{DenseMatrix => BDM, sum, DenseVector, min}
import org.apache.spark.graphx._
import org.apache.spark.graphx.util.GraphGenerators
import org.apache.log4j.Logger
import org.apache.log4j.Level

sc.setCheckpointDir("checkpoint/")

// [CHARLES] The following code did not need to be changed, except reformatting tabs

def generateGraph(n: Int, sc: SparkContext): Graph[Long, Double] = {
	val graph = GraphGenerators.logNormalGraph(sc, n).mapEdges(e => e.attr.toDouble)
	graph
}

def fromBreeze(dm: BDM[Double]): Matrix = {
  new DenseMatrix(dm.rows, dm.cols, dm.toArray, dm.isTranspose)
}

def toBreeze(A: Matrix): BDM[Double] = {
  new BDM[Double](A.numRows, A.numCols, A.toArray)
}

def localMinPlus(A: BDM[Double], B: BDM[Double]): BDM[Double] = {
  require(A.cols == B.rows, " Num cols of A does not match the num rows of B")
  val k = A.cols
  val onesA = DenseVector.ones[Double](B.cols)
  val onesB = DenseVector.ones[Double](A.rows)
  var AMinPlusB = A(::, 0) * onesA.t + onesB * B(0, ::)
  if (k > 1) {
    for (i <- 1 until k) {
      val a = A(::, i)
      val b = B(i, ::)
      val aPlusb = a * onesA.t + onesB * b
      AMinPlusB = min(aPlusb, AMinPlusB)
    }
  }
  AMinPlusB
}

def localFW(A: BDM[Double]): BDM[Double] = {
  require(A.rows == A.cols, "Matrix for localFW should be square!")
  var B = A
  val onesA = DenseVector.ones[Double](A.rows)
  for (i <- 0 until A.rows) {
    val a = B(::, i)
    val b = B(i, ::)
    B = min(B, a * onesA.t + onesA * b)
  }
  B
}

// [CHARLES] Main code

val n = 4
val m = 2
val stepSize = 1
val graph = generateGraph(n, sc)

// [CHARLES] Write a new function to generate Input
val RowsPerBlock = m // function arg
val ColsPerBlock = m // function arg
val entries = graph.edges.map { case edge => MatrixEntry(edge.srcId.toInt, edge.dstId.toInt, edge.attr) }
val coordMat = new CoordinateMatrix(entries, n, n)
val matA = coordMat.toBlockMatrix(RowsPerBlock, ColsPerBlock)


//val matA = generateInput(graph, n, sc, m, m)



