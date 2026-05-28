# Scripts
___

Utility scripts for building lamag and preparing test fixtures.

## Entrypoint scripts
1. [build_native.sh](build_native.sh) - Build the lamag native-image binary via the `native` Maven profile.
2. [make_examples.sh](make_examples.sh) - Copy regression fixtures (`*.lama`, `*.input`, `*.t`) from the Lama submodule into `regression/`.
3. [make_perf.sh](make_perf.sh) - Copy performance fixtures (`*.lama`, `*.input`) from the Lama submodule into `performance/`.
4. [clean_temp.sh](clean_temp.sh) - Remove fixtures copied into `regression/` and `performance/`.
5. [docker_setup.sh](docker_setup.sh) - Build the `dlama:1.0.0` image used to obtain `lamac` on ARM/Mac.

## Helper scripts
1. [make_suite.sh](make_suite.sh) - Shared helper that copies fixtures matching given globs from a source to a destination directory.
