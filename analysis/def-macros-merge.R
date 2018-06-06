#!/usr/bin/env Rscript

library(tidyverse)
library(pbapply)
library(parallel)

count_def_macros <- function(path) {
  if (file.exists(path)) {
    tryCatch({
      suppressWarnings(df <- read_csv(path, col_names=c("path", "n", "error"), col_types=cols("c", "i", "c")))
      stop_for_problems(df)
      sum(filter(df, n > 0)$n)
    }, error=function(e) {
      e$message
    })
  } else {
    NA
  }
}

projects <- readLines("/var/lib/scala/projects.txt")
projects_gh <- str_replace(projects, "^(.*)--(.*)$", "\\1/\\2")

csvs <- file.path("/var/lib/scala/projects", projects, "_analysis_", "def-macros-output.csv")
nopt <- pblapply(csvs, count_def_macros, cl=detectCores())
n <- pbsapply(nopt, function(x) if (is.integer(x)) x else NA)
error <- pbsapply(nopt, function(x) if (is.character(x)) x else NA)

df <- data_frame(project=projects_gh, n=n, url=str_c("https://github.com/", projects_gh), error=error, path=csvs)

write_csv(df, "def-macros-merged.csv")