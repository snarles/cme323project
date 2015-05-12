import org.apache.spark.mllib.linalg.distributed.{BlockMatrix, CoordinateMatrix, MatrixEntry}
import org.apache.spark.mllib.linalg.{Matrix, Matrices}
val entries = sc.parallelize(Array((0, 1, 20), (0, 2, 4), (0, 3, 2), 
(1, 0, 2), (1, 2, 1), (1, 3, 3),(2, 0, 1),
(2,1, 6), (2, 3, 5), (3, 0, 4), (3, 1, 2), (3, 2, 2))).map{case (i, j, v) => 
                                                                MatrixEntry(i, j, v)}
val coordMat = new CoordinateMatrix(entries)
val matA = coordMat.toBlockMatrix(2, 2).cache()
val n1 = matA.rowsPerBlock
val p1 = matA.numRowBlocks
val n = matA.numRows.toInt
matA.validate()

def crossPlus (v1:Array[Double], v2:Array[Double]): Array[Double] = {
    var v = Array[Double]()
    for (i <- 0 to (v2.length - 1)) {
       var temp = v1.clone()
       for (j <- 0 to (v1.length - 1)) {temp.update(j, v1(j) + v2(i))}
       v = v ++ temp
    }
    return v
}

def compareMin (v1:Array[Double], v2:Array[Double]): Array[Double] = {
   var v = v1.clone()
   for (i <- 0 to (v1.length - 1)) {
      if (v2(i) < v1(i)) v.update(i, v2(i))
   }
   return v
}

var edgeRdd = matA.blocks.map{case ((i, j), sub) => ((i, j), (sub.toArray,
                                                              sub.transpose.toArray))}

for (i <- 0 to (n-1)) {
  println(i)
  val k1 = i/n1
  val k2 = i - k1 * n1
  val rowRdd = edgeRdd.filter(_._1._1 == k1).map(block => (i, (block._1._2, 
                                                block._2._2.slice(k2 * n1, k2 * n1 + n1))))
  val rowRdd2 = edgeRdd.filter(_._1._2 == k1).map(block => (i, (block._1._1, 
                                                block._2._1.slice(k2 * n1, k2 * n1 + n1))))
  val joinRdd = rowRdd.join(rowRdd2).map{case (k, ((i, v1), (j, v2))) => 
                                                ((j, i), crossPlus(v2, v1))}
  edgeRdd = edgeRdd.join(joinRdd).mapValues{case ((v1, v2), v3) => (compareMin(v1, v3), 
            Matrices.dense(n1, n1, compareMin(v1, v3)).transpose.toArray)}
}

edgeRdd.map{case ((i, j), (v1, v2)) => ((i, j), Matrices.dense(n1, n1, v1))}.collect()
