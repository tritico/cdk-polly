#!/bin/bash
exec java -cp target/classes:$(cat .classpath.txt) tritico.eval.CdkPollyEval app $@