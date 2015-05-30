####
## Plots of FW and Block APSP in circle form
####

n <- 5 # number of vertices
a <- circle_graph(n)
pdf("figures/fig1_00.pdf", width = 7, height = 7)
plotg(a)
dev.off()

blocksize <- 1
iter <- 1
