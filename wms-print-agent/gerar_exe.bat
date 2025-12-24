jpackage --input target/ \
  --name "WMSPrintAgent" \
  --main-jar wms-print-agent-0.0.1-SNAPSHOT.jar \
  --main-class br.com.hacerfak.printagent.WmsPrintAgentApplication \
  --type app-image \
  --win-console \
  --dest dist