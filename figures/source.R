# Floyd Warshall
fw <- function(a) {
  n <- dim(a)[1]; inds <- 1:n
  for (k in inds) {
    for (i in inds) {
      for (j in inds) {
        a[i, j] <- min(a[i, j], a[i, k] + a[k, j])
      }
    }
  }
  a
}

# Min plus mult
minplus <- function(a, b, ans = matrix(Inf, dim(a)[1], dim(b)[2])) {
  m <- dim(a)[1]
  k <- dim(a)[2]
  n <- dim(b)[2]
  for (i in 1:m) {
    for (j in 1:n) {
      ans[i, j] <- min(ans[i, j], a[i, ] + b[, j])
    }
  }
  ans
}

# yields the adjacency matrix of the circle graph
circle_graph <- function(n) {
  ans <- matrix(Inf, n, n)
  ans[row(ans) == col(ans)] <- 0
  ans[abs(row(ans) - col(ans)) == 1] <- 1
  ans[n, 1] <- ans[1, n] <- 1
  ans
}

# plots the circle graph with given edges
plotg <- function(a, lwd = 1, cc = "black", pch = "o", cex = 1,
                  inds = c(), cca = "red", cexa = 2) {
  n <- dim(a)[1]
  xs <-cos(1:n/n * 2 * pi)
  ys <- sin(1:n/n * 2 * pi)
  plot(xs, ys, pch = pch, cex = cex, xlab = "", ylab = "", axes = FALSE,
       xlim = 1.1 * c(-1, 1), ylim = 1.1 * c(-1, 1))
  for (i in 1:n) {
    for (j in setdiff(1:n, i)) {
      if (a[i, j] < Inf) {
        lines(xs[c(i, j)], ys[c(i, j)], col = cc, lwd = lwd)
      }
    }
  }
  if (length(inds) > 0) {
    points(xs[inds], ys[inds], pch = pch, cex = cexa, col = cca)
  }
  for (i in 1:n) text(1.1 * xs[i], 1.1 * ys[i], paste(i), cex = cex)
}

# highlight vertices (not used)
ploth <- function(a, inds, lwd = 1, rad = 0.05, res = 5, cc = "red") {
  n <- dim(a)[1]
  xs <-cos(1:n/n * 2 * pi)
  ys <- sin(1:n/n * 2 * pi)
  ind1 <- min(inds)
  ind2 <- max(inds)
  mm <- 1/n * 2 * pi
  thetas <- 0:res/res * 2 * pi
  # circle left
  lines(xs[ind1] + rad * cos(thetas/2 + mm * ind1 + pi),
        ys[ind1] + rad * sin(thetas/2 + mm * ind1 + pi),
        lwd = lwd, col = cc)
  # circle right
  lines(xs[ind2] + rad * cos(thetas/2 + mm * ind2),
        ys[ind2] + rad * sin(thetas/2 + mm * ind2),
        lwd = lwd, col = cc)
  # connecting
  if (ind2 > ind1) {
    lines((1 + rad) * cos(seq(ind1, ind2, 1/res) * mm),
          (1 + rad) * sin(seq(ind1, ind2, 1/res) * mm),
          lwd = lwd, col = cc)
    lines((1 - rad) * cos(seq(ind1, ind2, 1/res) * mm),
          (1 - rad) * sin(seq(ind1, ind2, 1/res) * mm),
          lwd = lwd, col = cc)    
  }
}

# plots newly added edges
plotl <- function(a, a_new, lwd = 2, cc = "blue", pch = "o", 
                  inds = c(), cca = "red", cexa = 2) {
  n <- dim(a)[1]
  xs <-cos(1:n/n * 2 * pi)
  ys <- sin(1:n/n * 2 * pi)
  tvals <- (a_new < Inf) & (a == Inf)
  newedges <- cbind(row(a)[tvals], col(a)[tvals])
  for (x in data.frame(t(newedges))) {
    if (x[1] > x[2]) {
      lines(xs[x], ys[x], col = cc, lwd = lwd)
    }
  }
  if (length(inds) > 0) {
    points(xs[inds], ys[inds], pch = pch, cex = cexa, col = cca)
  }
}



