#!/bin/bash

if [ -z "$FAC_ADAPTER_ROOT" ]; then
  export FAC_ADAPTER_ROOT=`pwd` # try pwd
fi

MEMORY=10g

$FAC_ADAPTER_ROOT/bin/run_class.sh -Xmx$MEMORY adapter.Adapter \
--inputDB predsynth \
--input-collection papers \
--port-num 27017 \
--port-name localhost \
--outputDB predsynth \
--output-collection parsed_papers \
--maps-dir standard-psr-chen-collobert \
--model model-ptb-standard-psr-chen-collobert.torch.hd5
