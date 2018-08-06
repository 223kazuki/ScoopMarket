#!/bin/bash

lein do clean, build
IPFS_HASH=`ipfs add -r resources/public | tail -1 | cut -d' ' -f2`
INDEX_HASH=`cat resources/index_ipfs.html.tmpl| sed "s/{{IPFS_HASH}}/"$IPFS_HASH"/g" | ipfs add | cut -d' ' -f2`
open http://localhost:8080/ipfs/$INDEX_HASH
# open https://gateway.ipfs.io/ipfs/$INDEX_HASH
