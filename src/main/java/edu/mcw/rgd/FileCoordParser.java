package edu.mcw.rgd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * @author KPulakanti
 * @since Jun 29, 2010
 * 1. MGI Marker Accession ID	2. Marker Type	3. Feature Type	4. Marker Symbol	5. Marker Name
 * 6. Chromosome	7. Start Coordinate	8. End Coordinate	9. Strand	10. Genome Build
 * 11. Provider Collection	12. Provider Display
 *
 * sample line
 * MGI:87906	Gene	protein coding gene	Actg1	actin, gamma, cytoplasmic 1	11	120345690	120348542	-	GRCm38	VEGA Gene Model	VEGA
 */
public class FileCoordParser extends FileParser {

    private Set<String> knownGenomeBuilds;

    Logger log = LogManager.getLogger("status");

    public void parseLine(String[] columns) throws Exception {

        // if there is not enough columns, skip the line
        if( columns.length<12 )
            return;

        // 2nd column contains the object type, like 'Gene' or 'Qtl'
        String objType = columns[1];
        if( !objType.startsWith("Q") )
            return;

        // todo: validate the data here
        //
        //
        String mgiId = columns[0].trim();

        // get the existing QtlData object from the map
        QtlData data = qtlDataMap.get(mgiId);

        // if there is no allele data, we skip this line
        if( data==null )
            return;

        // if there is no genomic positions available, return
        String start = columns[6];
        String stop = columns[7];
        if( start.equals("null") || stop.equals("null") )
            return;

        data.setMarkname(columns[3]);
        data.setStart(start);
        data.setStop(stop);
        data.setChromosome(columns[5]);  //NCBI gene chromosome coming from Coordinate.rpt list
        data.setStrand(columns[8]);
        data.setGenomeBuild(columns[9]);

        if( !getKnownGenomeBuilds().contains(data.getGenomeBuild()) ) {
            throw new Exception("ERROR: Unknown genome build: "+data.getGenomeBuild()+"; add the new genome build to AppConfigure.xml file");
        }

        log.debug(mgiId+" markname="+columns[3]+" chr="+data.getChromosome()+" start="+data.getStart()+" stop="+data.getStop());
    }

    public Set<String> getKnownGenomeBuilds() {
        return knownGenomeBuilds;
    }

    public void setKnownGenomeBuilds(Set<String> knownGenomeBuilds) {
        this.knownGenomeBuilds = knownGenomeBuilds;
    }
}

