# @author sebastiano panichella

#install packages if not installed yet
if (!require(tm)){ install.packages("tm") }
if (!require(stringr)){ install.packages("stringr") } 
if (!require(stopwords)){ install.packages("stopwords") }
if (!require(slam)){ install.packages("slam") }
if (!require(snakecase)){ install.packages("snakecase") }
if (!require(data.table)){ install.packages("data.table") }
if (!require(XML)){ install.packages("XML") }

#load the libraries...
library(tm)
library(stringr)
library(stopwords)
library(slam)

base_folder <- setwd("~/Desktop/Zurich-applied-Science/Collaborations/UniBE/Collaborations/Pooja Rani/comment-analysis-neon/tf-idf-scripts")

source("./Scripts/utilities.R")

#path software artifacts
mydir1 <- "./Scripts/EasyClinic-Data/documents/1 - use cases"
mydir2 <- "./Scripts/EasyClinic-Data/documents/4 - class description"
# creating folders with pre-processed documents (e.g., camel case splitting, etc.)
mydir1_prepocessed  <- "./Scripts/EasyClinic-Data/documents-preprocessed/1 - use cases"
mydir2_prepocessed  <- "./Scripts/EasyClinic-Data/documents-preprocessed/4 - class description"

#
pre_processing(mydir1, mydir1_prepocessed, ".txt")
pre_processing(mydir2, mydir2_prepocessed, ".txt")

# directories to parse
directories <- c(mydir1_prepocessed, mydir2_prepocessed)

# the following command index the software artifacts
# and store this data in "tm" as sparse matrix
tdm <- build_tm_matrix(directories)
tdm_full <- as.matrix(tdm)

tdm_full2<- t(tdm_full)

write.csv(tdm_full2,"./Scripts/EasyClinic-Data/matrix.csv",quote = FALSE)





