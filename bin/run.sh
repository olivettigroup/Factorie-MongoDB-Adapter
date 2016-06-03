#!/bin/bash

MEMORY=10g

$FAC_ADAPTER_ROOT/bin/run_class.sh -Xmx$MEMORY edu.umass.cs.iesl.lffi.parse.TransitionBasedParserTrainer --option=value
