#!/bin/bash

MEMORY=10g

$SPECIFIC_PROJECT_ROOT/bin/run_class.sh -Xmx$MEMORY edu.umass.cs.iesl.lffi.parse.TransitionBasedParserTrainer --option=value