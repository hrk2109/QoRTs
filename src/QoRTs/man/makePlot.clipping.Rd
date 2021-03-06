\name{makePlot.clipping}
\docType{methods}
\alias{makePlot.clipping}
\title{
   Plot Alignment Clipping
}
\description{
   Plots the rate at which the aligner soft-clips off portions of
   aligned reads.
}
\usage{
  makePlot.clipping(plotter, 
                 rate.per.million = FALSE,
                 use.readLength.denominator = TRUE, 
                 r2.buffer,
                 debugMode, 
                 singleEndMode, 
                 rasterize.plotting.area = FALSE, 
                 raster.height = 1000, 
                 raster.width = 1000, 
                 plot= TRUE, ...)
}
\arguments{
  \item{plotter}{
    A \code{QoRT_Plotter} reference object. See \code{\link{build.plotter}}.
  }
  \item{rate.per.million}{
    A logical value indicating whether or not to scale the y 
    axis to rate-per-million-reads, rather than rate-per-read.
    Some people may find the results more readable this way, even
    though the plots themselves will appear the same.
  }
  \item{use.readLength.denominator}{
    Logical. If TRUE, then use the total number of reads with at least the given position's length as the denominator of the rate. This is only relevant when the dataset has variable-length trimming prior to alignment.
  }
  \item{r2.buffer}{
    Buffer space to place between the plotting of read 1
    and read 2. By default this will choose a reasonable value.
  }
  \item{debugMode}{
    Logical. If TRUE, debugging data will be printed to the console.
  }
  \item{singleEndMode}{
      Logical. Determines whether the dataset consists of single-ended reads. 
      By default this is determined from the data. Thus, this parameter should 
      never need to be set by the user.
  }
  \item{rasterize.plotting.area}{
      Logical. If \code{TRUE}, then "flatten" the plotting lines into a raster format. 
      This requires support for png file creation and the installation of the "png" 
      package. Only the plotting lines will be rasterized, the 
      axes and annotations will be vector format. Default is \code{FALSE}.
  }
  \item{raster.height}{
      Numeric integer. If \code{rasterize.plotting.area} is \code{TRUE}, then this
      will set the height of the rasterized plot, in pixels.
  }
  \item{raster.width}{
      Numeric integer. If \code{rasterize.plotting.area} is \code{TRUE}, then this
      will set the width of the rasterized plot, in pixels.
  }
  \item{plot}{
        Logical. If FALSE, suppress plotting and return \code{TRUE} if and only if the data is present in the dataset to allow plotting. Useful to automatically filter out missing data plots.
  }
  \item{\dots}{ 
    Arguments to be passed to methods, such as
    \link{graphical parameters} (see \code{\link{par}}).
  }
}
\details{
  x-axis: The read cycle (ie. the base-pair position in the read).
  
  y-axis: The rate at which the bases at the given read-cycle is
  clipped off.
}
\examples{
  data(res,package="QoRTsExampleData");
  plotter <- build.plotter.colorByGroup(res);
  makePlot.clipping(plotter)
}
\seealso{
  \code{\link{build.plotter}}
}