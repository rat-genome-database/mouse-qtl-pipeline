package edu.mcw.rgd;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author KPulakanti
 * @since Jun 29, 2010
 * <p>
 * Parse the file and extract PubMed IDs, MGI Ids, qtl symbols and MP Ids
 */
public class FileAlleleParser extends FileParser {

    Logger log = Logger.getLogger("core");

    /**
     * parse a line for the following information: old version of file
     * <ol>
     *     <li>PubMed ID should be in 5th column</li>
     *     <li>MGI ID should be in 6th column</li>
     *     <li>symbol should be in 7th column -- we add suffix '_m' to differentiate qtl symbol from rat/human qtls</li>
     *     <li>list of comma separated MP ids is found in 10th column</li>
     * </ol>
     * new version of file, starting Dec 6, 2012:
     * <ol>
     *     <li>MGI Allele Accession ID</li>
     *     <li>Allele Symbol</li>
     *     <li>Allele Name</li>
     *     <li>Allele Type</li>
     *     <li>PubMed ID</li>
     *     <li>MGI Marker Accession ID</li>
     *     <li>Marker symbol -- we add suffix '_m' to differentiate qtl symbol from rat/human qtls</li>
     *     <li>Marker RefSeq ID</li>
     *     <li>Marker Ensembl ID</li>
     *     <li>Marker Chromosome</li>
     *     <li>Marker Start Coordinate</li>
     *     <li>Marker End Coordinate</li>
     *     <li>Genome Build</li>
     *     <li>list of comma separated MP ids</li>
     * </ol>
     * @param columns line columns
     */
    public void parseLine(String[] columns) throws Exception {

        // if there is not enough columns, skip the line
        if( columns.length!=14 )
            return;

        // there are multiple lines with same MGI id
        String mgiId = columns[5];
        String pubMedId = columns[4];

        // symbols must have a suffix '_m' to differentiate them from rat/human qtls
        String symbol = columns[6] + "_m";

        List<String> mpIdList = new ArrayList<String>( Arrays.asList(columns[13].split(",")) );

        // look for existing qtl
        QtlData data = qtlDataMap.get(mgiId);
        if( data==null ) {
            // new MGI id
            data = new QtlData();
            data.setMgiId(mgiId);
            data.setSymbol(symbol);
            data.setMpIdList(mpIdList);

            List<String> pubMedIdList = new ArrayList<String>();
            // do not insert null pubmed ids
            if( pubMedId.trim().length()> 0 )
                pubMedIdList.add(pubMedId);
            data.setPmIdList(pubMedIdList);

            qtlDataMap.put(mgiId, data);
        }
        else {
            // existing mgi id -- compare the incoming data against the data already read-in
            //
            // compare symbols
            if( !data.getSymbol().equals(symbol) ) {
                log.error("different symbol "+symbol+" for QTL "+mgiId+" with symbol "+data.getSymbol());
                return;
            }

            // compare PubMed Ids: if new pubmed id found, add it to list
            if( pubMedId.trim().length()> 0 ) {
                if( !data.getPmIdList().contains(pubMedId) )
                    data.getPmIdList().add(pubMedId);
            }

            // compare 'MP:' ids -- new MP ids should be added into the list
            for( String mpId: mpIdList ) {
                if( !data.getMpIdList().contains(mpId) )
                    data.getMpIdList().add(mpId);
            }
        }

        String markerChr = columns[9].trim();
        String markerStartPos = columns[10].trim();
        String markerStopPos = columns[11].trim();
        String genomeBuild = columns[12].trim();

        if( !markerChr.equals("null") ||
            !markerStartPos.equals("null") ||
            !markerStopPos.equals("null") ||
            !genomeBuild.equals("null") ) {

            data.setChromosome(markerChr);
            data.setStart(markerStartPos);
            data.setStop(markerStopPos);
            data.setGenomeBuild(genomeBuild);
        }
    }
}
