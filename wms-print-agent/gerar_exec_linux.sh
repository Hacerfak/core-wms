jpackage \
  --input target/ \
  --name "wms-print-agent" \
  --main-jar wms-print-agent-1.0.0.jar \
  --type app-image \ 
  --dest dist \
  --java-options "-Dfile.encoding=UTF-8"

  ## da pra trocar --type app-image por --type deb para gerar um pacote.