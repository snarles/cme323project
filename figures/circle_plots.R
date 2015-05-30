####
## Plots of FW and Block APSP in circle form
####

## FW with 6

n <- 6 # number of vertices
a <- circle_graph(n)
png("figures/fig1_00.png", width = 800, height = 800)
plotg(a, cex = 3, lwd = 3)
dev.off()

blocksize <- 1
for (it in 1:n) {
  png(paste0("figures/fig1_0", it, ".png"), width = 800, height = 800)
  inds <- (blocksize * (it-1) + 1):(blocksize * it)
  a_new <- a
  a_new[inds, inds] <- fw(a[inds, inds, drop = FALSE])
  a_new <- minplus(a[, inds, drop = FALSE], a[inds, , drop = FALSE], a)
  plotg(a, cex = 3, lwd = 3, cexa = 5, inds = inds)
  plotl(a, a_new, lwd = 5, cexa = 5, inds = inds)
  a <- a_new
  #title(paste("Iteration", it))
  dev.off()
}


## Block with 24

n <- 24
blocksize <- 8
a <- circle_graph(n)

png("figures/fig2_00.png", width = 800, height = 800)
plotg(a)
dev.off()
png("figures/fig2_00m.png", width = 800, height = 800)
plotm(a, a)
dev.off()


for (it in 1:3) {
  
  inds <- (blocksize * (it-1) + 1):(blocksize * it)
  a_new <- a
  a_new[inds, inds] <- fw(a[inds, inds, drop = FALSE])
  png(paste0("figures/fig2_0", it ,"a.png"), width = 800, height = 800)
  plotg(a, inds = inds, cexa = 3)
  plotl(a, a_new, inds = inds, cexa = 3, lwd = 3)
  dev.off()
  png(paste0("figures/fig2_0", it ,"am.png"), width = 800, height = 800)
  plotm(a, a_new)
  dev.off()
  a <- a_new
  
  a_new[inds, ] <- minplus(a[inds, inds, drop = FALSE], a[inds, , drop = FALSE],
                           a[inds, , drop = FALSE])
  a_new[, inds] <- minplus(a[, inds, drop = FALSE], a[inds, inds, drop = FALSE],
                           a[, inds, drop = FALSE])
  png(paste0("figures/fig2_0", it ,"b.png"), width = 800, height = 800)
  plotg(a, inds = inds, cexa = 3)
  plotl(a, a_new, inds = inds, cexa = 3, lwd = 3)
  dev.off()
  png(paste0("figures/fig2_0", it ,"bm.png"), width = 800, height = 800)
  plotm(a, a_new)
  dev.off()
  a <- a_new
  
  a_new <- minplus(a[, inds, drop = FALSE], a[inds, , drop = FALSE], a)
  png(paste0("figures/fig2_0", it ,"c.png"), width = 800, height = 800)  
  plotg(a, inds = inds, cexa = 3)
  plotl(a, a_new, inds = inds, cexa = 3, lwd = 3)
  dev.off()
  png(paste0("figures/fig2_0", it ,"cm.png"), width = 800, height = 800)
  plotm(a, a_new)
  dev.off()
  a <- a_new
  
  
}


