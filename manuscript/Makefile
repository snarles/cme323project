all :
	make clean || true
	pdflatex apsp_path.tex 
	bibtex apsp_path 
	pdflatex apsp_path.tex 
	pdflatex apsp_path.tex 

clean :
	rm *.aux *.log *.bbl *.blg
