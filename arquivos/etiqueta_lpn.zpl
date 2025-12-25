^XA 

^FT30,50^A0N,25,25^FH\^CI28^FDNFe Entrada^FS^CI27 
^FT30,115^A0N,55,55^FB320,1,0,L,0^FH\^CI28^FD99999999/1^FS^CI27
^FX Aqui é o número da NF-e de entrada mais a série, já com o limite máximo de numeração da NF-e ^FS

^FT350,50^A0N,25,25^FH\^CI28^FDProduto^FS^CI27   
^FT350,115^A0N,55,55^FB450,1,0,L,0^FH\^CI28^FD{{SKU}}^FS^CI27
^FX Aqui é o código SKU do produto ^FS
  
^FT30,180^A0N,35,35^FB750,1,0,C,0^FH\^CI28^FD{{DESC}}^FS^CI27
^FX Aqui é a descrição do produto, limitado a 40 caracteres ^FS

^FT30,235^A0N,28,28^FH\^CI28^FDLOTE^FS^CI27
^FT30,300^A0N,50,50^FB300,1,0,L,0^FH\^CI28^FD{{LOTE}}^FS^CI27
^FX Aqui é o lote do produto ^FS

^FT350,235^A0N,28,28^FH\^CI28^FDENTRADA^FS^CI27
^FT315,300^A0N,55,55^FB200,1,0,L,0^FH\^CI28^FD12/08/2025^FS^CI27
^FX Aqui é a data de validade no formato DD/MM/AA^FS 

^FT650,235^A0N,28,28^FH\^CI28^FDQTD^FS^CI27
^FT710,240^A0N,28,28^FB75,1,0,L,0^FH\^CI28^FD{{TIPO}}^FS^CI27
^FX Aqui é a unidade de medida do produto, limitado a 4 caracteres ^FS

^FT490,300^A0N,55,55^FB300,1,0,R,0^FH\^CI28^FD{{QTD}}^FS^CI27
^FX Aqui é a quantidade do produto^FS

^FT30,330^A0N,25,25^FH\^CI28^FDDEPOSITANTE^FS^CI27
^FT30,365^A0N,25,25^FB600,1,0,L,0^FH\^CI28^FDLOREM IPSUM DOLOR LOREM IPSUM DOLOR LOREM IPS^FS^CI27 
^FX Aqui é a descrição do depositante, limitado a 45 caracteres ^FS
 
^FT650,330^A0N,25,25^FH\^CI28^FDVALIDADE^FS^CI27 ^FT565,365^A0N,30,30^FB190,1,0,R,0^FH\^CI28^FD{{VALIDADE}}^FS^CI27
^FX Aqui é a data de recebimento da nota no formato DD/MM/AA^FS

^BY6,3,110^FT60,490^BCN,110,Y,N,N,N^FH\^FD{{LPN_CODIGO}}^FS
^FX Aqui é o código de barras da LPN^FS

^LRY^FO0,125^GB800,75,75^FS
^FX Aqui é a faixa preta na descrição do nome do produto ^FS

^XZ