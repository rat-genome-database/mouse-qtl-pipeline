package edu.mcw.rgd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author KPulakanti
 * @since Jun 29, 2010
 * <p>
 *  Sample header line and data line<pre>
 MGI Accession ID	Chr	cM Position	genome coordinate start	genome coordinate end	strand	Marker Symbol	Status	Marker Name	Marker Type	Feature Type	Marker Synonyms (pipe-separated)
 MGI:3042396	2	     58.52	116028484	116028676	+	Aabpr	O	aberrant activation of B cell proliferation	QTL	QTL	Abpr
 </pre>
 */
public class FileMrkListParser extends FileParser {

    Logger log = LogManager.getLogger("status");

    public void parseLine(String[] columns) throws Exception {

        // skip comment lines or invalid lines
        if( columns.length!=12 ) {
            log.error("Invalid format for MRK_List file! It must have exactly 12 columns!");
        }

        // validate status
        String status = columns[7];
        if( status.length()!=1 )
            return; // status must be exactly one character long
        if( status.equals("W") )
            return; // ignore withdrawn objects

        // object type must be qtl
        String markerType = columns[9].trim().toLowerCase();
        if( !markerType.equals("qtl") )
            return;

        String mgiId = columns[0];

        if(mgiId.startsWith("MGI:")){
            String chrnum = columns[1];
            String cmposition = columns[2];
            String name = columns[8];

            String start = columns[3];
            String stop = columns[4];
            String strand = columns[5];

            QtlData data = qtlDataMap.get(mgiId);
            if( data==null ) {
                //System.out.println("MGIID "+mgiId+" not found in the map!!!");
            }
            else {
                data.setChrnum(chrnum);
                data.setCmposition(cmposition);

                // qtl name should have a suffix " (mouse)"
                data.setName(name + " (mouse)");

                data.setStart(start);
                data.setStop(stop);
                data.setStrand(strand);
            }
        }

    }

}
        