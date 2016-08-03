#!/bin/bash

if [ -z "$FAC_ADAPTER_ROOT" ]; then
  export FAC_ADAPTER_ROOT=`pwd` # try pwd
fi

MEMORY=10g

$FAC_ADAPTER_ROOT/bin/run_class.sh -Xmx$MEMORY adapter.Adapter \
--inputDB predsynth \
--input-collection paragraphs \
--port-num 27017 \
--port-name localhost \
--outputDB predsynth \
--output-collection parsed_papers \
--maps-dir /home/umaguest/intmaps/standard-psr-chen-collobert \
--model /home/umaguest/model-ptb-standard-psr-chen-collobert.torch.hd5
